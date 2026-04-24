package com.workouthub.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workouthub.auth.dto.AuthResponse;
import com.workouthub.users.domain.Role;
import com.workouthub.users.domain.User;
import com.workouthub.users.domain.UserProfile;
import com.workouthub.users.domain.UserProfileRepository;
import com.workouthub.users.domain.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Minimal hand-rolled OIDC client for an optional Authentik (or any
 * OIDC-compliant) identity provider. Activated with
 * {@code app.auth.oidc.enabled=true}. Disabled by default so the rest
 * of the stack works without configuring an IdP.
 *
 * <p>The callback finds or creates a local User by email and returns
 * the standard AuthResponse - same shape as /api/auth/login - so the
 * frontend integration is identical.
 */
@RestController
@RequestMapping("/api/auth/oidc")
public class OidcController {

    private static final SecureRandom RNG = new SecureRandom();

    private final boolean enabled;
    private final String issuerUrl;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final boolean autoProvision;

    private final UserRepository users;
    private final UserProfileRepository profiles;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public OidcController(
            @Value("${app.auth.oidc.enabled:false}") boolean enabled,
            @Value("${app.auth.oidc.issuer-url:}") String issuerUrl,
            @Value("${app.auth.oidc.client-id:}") String clientId,
            @Value("${app.auth.oidc.client-secret:}") String clientSecret,
            @Value("${app.auth.oidc.redirect-uri:}") String redirectUri,
            @Value("${app.auth.oidc.auto-provision:true}") boolean autoProvision,
            UserRepository users,
            UserProfileRepository profiles,
            PasswordEncoder passwordEncoder,
            AuthService authService,
            ObjectMapper objectMapper) {
        this.enabled = enabled;
        this.issuerUrl = trimSlash(issuerUrl);
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.autoProvision = autoProvision;
        this.users = users;
        this.profiles = profiles;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/login")
    public ResponseEntity<Void> login() {
        if (!enabled) return ResponseEntity.notFound().build();
        requireConfigured();
        String state = randomToken();
        String url = issuerUrl + "/application/o/authorize/"
                + "?client_id=" + enc(clientId)
                + "&redirect_uri=" + enc(redirectUri)
                + "&response_type=code"
                + "&scope=openid+profile+email"
                + "&state=" + enc(state);
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(url)).build();
    }

    @GetMapping("/callback")
    @Transactional
    public AuthResponse callback(
            @RequestParam("code") String code,
            @RequestParam(value = "state", required = false) String state,
            HttpServletRequest httpReq) {
        if (!enabled) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        requireConfigured();

        String accessToken = exchangeCodeForAccessToken(code);
        JsonNode userinfo = fetchUserInfo(accessToken);
        String email = textOrNull(userinfo, "email");
        String name = textOrDefault(userinfo, "name",
                textOrDefault(userinfo, "preferred_username", email));
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "userinfo missing email claim");
        }

        User user = users.findByEmailIgnoreCase(email).orElse(null);
        if (user == null) {
            if (!autoProvision) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "no local user for " + email + " and auto-provision disabled");
            }
            user = new User();
            user.setEmail(email);
            user.setDisplayName(name);
            user.setRole(Role.USER);
            // OIDC users still carry a password hash so the row satisfies
            // the NOT NULL constraint; they cannot log in with it.
            user.setPasswordHash(passwordEncoder.encode(randomToken()));
            user = users.save(user);

            UserProfile profile = new UserProfile();
            profile.setUser(user);
            profiles.save(profile);
        }

        return authService.issueTokensForOidc(user, httpReq.getHeader("User-Agent"));
    }

    private String exchangeCodeForAccessToken(String code) {
        String body = "grant_type=authorization_code"
                + "&code=" + enc(code)
                + "&redirect_uri=" + enc(redirectUri)
                + "&client_id=" + enc(clientId)
                + "&client_secret=" + enc(clientSecret);
        HttpRequest req = HttpRequest.newBuilder(URI.create(issuerUrl + "/application/o/token/"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        try {
            HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() / 100 != 2) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "token exchange failed: " + res.statusCode());
            }
            return objectMapper.readTree(res.body()).get("access_token").asText();
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "token exchange error: " + ex.getMessage());
        }
    }

    private JsonNode fetchUserInfo(String accessToken) {
        HttpRequest req = HttpRequest.newBuilder(URI.create(issuerUrl + "/application/o/userinfo/"))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();
        try {
            HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() / 100 != 2) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "userinfo fetch failed: " + res.statusCode());
            }
            return objectMapper.readTree(res.body());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "userinfo fetch error: " + ex.getMessage());
        }
    }

    private void requireConfigured() {
        if (issuerUrl.isBlank() || clientId.isBlank() || redirectUri.isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "OIDC enabled but issuer-url / client-id / redirect-uri is missing");
        }
    }

    private static String trimSlash(String s) {
        if (s == null) return "";
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    private static String enc(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }

    private static String textOrDefault(JsonNode node, String field, String fallback) {
        String v = textOrNull(node, field);
        return v == null ? fallback : v;
    }

    private static String randomToken() {
        byte[] bytes = new byte[24];
        RNG.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}

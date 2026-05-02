package com.workouthub.common.security;

import com.workouthub.users.domain.Role;
import com.workouthub.users.domain.User;
import com.workouthub.users.domain.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Trusts X-Forwarded-User / X-Forwarded-Email / X-Forwarded-Groups headers
 * when AUTH_MODE=forward-auth AND the request source IP is in TRUSTED_PROXIES.
 * Otherwise the headers are ignored entirely. The trust gate is non-optional
 * per docs/SELF_HOSTED_CONTRACT.md section 7.2.
 *
 * <p>Bean exists only when app.auth.mode=forward-auth so the built-in JWT
 * deployment path stays untouched in default mode.
 */
@Component
@ConditionalOnProperty(name = "app.auth.mode", havingValue = "forward-auth")
public class ForwardAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ForwardAuthFilter.class);

    private final TrustedProxyMatcher trustedProxies;
    private final UserRepository users;
    private final String headerUser;
    private final String headerEmail;
    private final String headerGroups;

    public ForwardAuthFilter(
            UserRepository users,
            @Value("${app.auth.trusted-proxies:}") String trustedProxiesCsv,
            @Value("${app.auth.headers.user:X-Forwarded-User}") String headerUser,
            @Value("${app.auth.headers.email:X-Forwarded-Email}") String headerEmail,
            @Value("${app.auth.headers.groups:X-Forwarded-Groups}") String headerGroups) {
        this.users = users;
        this.trustedProxies = new TrustedProxyMatcher(trustedProxiesCsv);
        this.headerUser = headerUser;
        this.headerEmail = headerEmail;
        this.headerGroups = headerGroups;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        if (!trustedProxies.isTrusted(req.getRemoteAddr())) {
            chain.doFilter(req, res);
            return;
        }
        String email = req.getHeader(headerEmail);
        String forwardedUser = req.getHeader(headerUser);
        if (email == null || email.isBlank()) {
            chain.doFilter(req, res);
            return;
        }
        Optional<User> userOpt = users.findByEmailIgnoreCase(email);
        if (userOpt.isEmpty()) {
            // Auto-provision is intentionally not implemented here; the operator
            // may add it later behind a separate flag. For now, unknown emails
            // simply do not authenticate; downstream Spring Security rules reject
            // the request at the protected resource. Logged at warn for visibility.
            log.warn("forward-auth: no local user for email={} forwardedUser={} groups={}",
                    email, forwardedUser, req.getHeader(headerGroups));
            chain.doFilter(req, res);
            return;
        }
        User user = userOpt.get();
        Role role = user.getRole();
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
        var principal = new AppUserPrincipal(user.getId(), role.name());
        var auth = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
        SecurityContextHolder.getContext().setAuthentication(auth);
        chain.doFilter(req, res);
    }
}

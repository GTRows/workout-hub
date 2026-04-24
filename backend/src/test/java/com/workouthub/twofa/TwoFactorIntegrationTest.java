package com.workouthub.twofa;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workouthub.support.AbstractIntegrationTest;
import com.workouthub.support.TestAuthHelpers;
import com.workouthub.support.TestAuthHelpers.SeededUser;
import com.workouthub.twofa.domain.UserTotp;
import com.workouthub.twofa.domain.UserTotpRepository;
import com.workouthub.users.domain.Role;
import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.time.SystemTimeProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
class TwoFactorIntegrationTest extends AbstractIntegrationTest {

    private static final String SECRET = "TwoFaSecret1!";
    private static final CodeGenerator CODE =
            new DefaultCodeGenerator(HashingAlgorithm.SHA1, 6);

    @Autowired MockMvc mvc;
    @Autowired TestAuthHelpers helpers;
    @Autowired UserTotpRepository totpRepo;
    @Autowired ObjectMapper objectMapper;

    @Test
    void statusIsNotEnrolledByDefault() throws Exception {
        SeededUser u = helpers.seed(
                "2fa-a-" + System.nanoTime() + "@test.local", SECRET, Role.USER);

        mvc.perform(get("/api/users/me/2fa")
                        .header("Authorization", "Bearer " + u.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enrolled").value(false))
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    void setupReturnsSecretAndOtpAuthUri() throws Exception {
        SeededUser u = helpers.seed(
                "2fa-b-" + System.nanoTime() + "@test.local", SECRET, Role.USER);

        MvcResult r = mvc.perform(post("/api/users/me/2fa/setup")
                        .header("Authorization", "Bearer " + u.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.secretBase32").isNotEmpty())
                .andExpect(jsonPath("$.otpAuthUri").value(
                        org.hamcrest.Matchers.startsWith("otpauth://totp/")))
                .andReturn();
        String body = r.getResponse().getContentAsString();
        String secret = objectMapper.readTree(body).get("secretBase32").asText();
        org.assertj.core.api.Assertions.assertThat(secret).isNotBlank();
    }

    @Test
    void verifyAcceptsCorrectCodeAndEnables2fa() throws Exception {
        SeededUser u = helpers.seed(
                "2fa-c-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        String auth = "Bearer " + u.accessToken();

        MvcResult setup = mvc.perform(post("/api/users/me/2fa/setup")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andReturn();
        String secret = objectMapper.readTree(setup.getResponse().getContentAsString())
                .get("secretBase32").asText();
        String code = CODE.generate(secret, currentBucket());

        mvc.perform(post("/api/users/me/2fa/verify")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + code + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));

        mvc.perform(get("/api/users/me/2fa").header("Authorization", auth))
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    void verifyRejectsWrongCode() throws Exception {
        SeededUser u = helpers.seed(
                "2fa-d-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        String auth = "Bearer " + u.accessToken();

        mvc.perform(post("/api/users/me/2fa/setup").header("Authorization", auth))
                .andExpect(status().isOk());

        mvc.perform(post("/api/users/me/2fa/verify")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"000000\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginRequiresTotpWhenEnabled() throws Exception {
        String email = "2fa-e-" + System.nanoTime() + "@test.local";
        SeededUser u = helpers.seed(email, SECRET, Role.USER);

        UserTotp t = new UserTotp();
        t.setUserId(u.id());
        t.setSecretBase32("JBSWY3DPEHPK3PXP");
        t.setEnabled(true);
        totpRepo.save(t);

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + SECRET + "\"}"))
                .andExpect(status().isUnauthorized());

        String code = CODE.generate("JBSWY3DPEHPK3PXP", currentBucket());
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + SECRET
                                + "\",\"totpCode\":\"" + code + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void disableRemovesTheFlagAndAllowsPasswordOnlyLogin() throws Exception {
        String email = "2fa-f-" + System.nanoTime() + "@test.local";
        SeededUser u = helpers.seed(email, SECRET, Role.USER);

        UserTotp t = new UserTotp();
        t.setUserId(u.id());
        t.setSecretBase32("JBSWY3DPEHPK3PXP");
        t.setEnabled(true);
        totpRepo.save(t);

        mvc.perform(delete("/api/users/me/2fa")
                        .header("Authorization", "Bearer " + u.accessToken()))
                .andExpect(status().isNoContent());

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + SECRET + "\"}"))
                .andExpect(status().isOk());
    }

    private static long currentBucket() {
        return new SystemTimeProvider().getTime() / 30;
    }
}

package dev.kekao.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kekao.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "kekao.auth.rate-limit.capacity=3",
        "kekao.auth.rate-limit.window=PT15M",
        "kekao.auth.access-token-ttl=PT15M",
        "kekao.auth.refresh-token-ttl=P30D"
})
@EnabledIf(value = "dev.kekao.DockerAvailability#isAvailable",
        disabledReason = "Docker daemon is not available; integration tests skipped")
class AuthIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;

    private String body(Object o) throws Exception {
        return json.writeValueAsString(o);
    }

    @Test
    void registerLoginAndRefreshHappyPath() throws Exception {
        String email = "user-" + System.nanoTime() + "@example.com";

        MvcResult reg = mvc.perform(post("/api/auth/register")
                        .with(req -> { req.setRemoteAddr("10.0.0.1"); return req; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("email", email, "password", "password123"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();

        String refreshToken = json.readTree(reg.getResponse().getContentAsString())
                .get("refreshToken").asText();

        mvc.perform(post("/api/auth/login")
                        .with(req -> { req.setRemoteAddr("10.0.0.2"); return req; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("email", email, "password", "password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        MvcResult refreshed = mvc.perform(post("/api/auth/refresh")
                        .with(req -> { req.setRemoteAddr("10.0.0.3"); return req; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isOk())
                .andReturn();

        String newRefresh = json.readTree(refreshed.getResponse().getContentAsString())
                .get("refreshToken").asText();
        assertThat(newRefresh).isNotEqualTo(refreshToken);

        // Old refresh is now revoked
        mvc.perform(post("/api/auth/refresh")
                        .with(req -> { req.setRemoteAddr("10.0.0.4"); return req; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registerRejectsInvalidEmailAndShortPassword() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .with(req -> { req.setRemoteAddr("10.0.1.1"); return req; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("email", "not-email", "password", "short"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"));
    }

    @Test
    void loginWrongPasswordReturns401() throws Exception {
        String email = "wrong-" + System.nanoTime() + "@example.com";
        mvc.perform(post("/api/auth/register")
                        .with(req -> { req.setRemoteAddr("10.0.2.1"); return req; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("email", email, "password", "password123"))))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/auth/login")
                        .with(req -> { req.setRemoteAddr("10.0.2.2"); return req; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("email", email, "password", "wrongpass"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rateLimitTriggersAfterCapacity() throws Exception {
        String ip = "10.0.99.99";
        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/api/auth/login")
                            .with(req -> { req.setRemoteAddr(ip); return req; })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(Map.of("email", "rl@example.com", "password", "whatever"))))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/auth/login")
                        .with(req -> { req.setRemoteAddr(ip); return req; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("email", "rl@example.com", "password", "whatever"))))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("rate_limited"));
    }
}

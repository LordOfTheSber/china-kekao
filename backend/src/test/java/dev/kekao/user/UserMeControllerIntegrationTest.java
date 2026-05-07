package dev.kekao.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kekao.AbstractPostgresIntegrationTest;
import dev.kekao.auth.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@EnabledIf(value = "dev.kekao.DockerAvailability#isAvailable",
        disabledReason = "Docker daemon is not available; integration tests skipped")
class UserMeControllerIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private UserRepository users;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private TokenService tokens;

    private UserEntity user;
    private String userToken;

    @BeforeEach
    void setUp() {
        users.deleteAll();
        user = users.save(UserEntity.builder()
                .email("settings-" + System.nanoTime() + "@e.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .role(UserRole.ROLE_USER)
                .settings(new HashMap<>())
                .build());
        userToken = tokens.issueAccessToken(user);
    }

    @Test
    void settingsRequireAuth() throws Exception {
        mvc.perform(get("/api/me/settings")).andExpect(status().isUnauthorized());
    }

    @Test
    void getReturnsDefaults() throws Exception {
        mvc.perform(get("/api/me/settings")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newPerDay").value(UserSettings.DEFAULT_NEW_PER_DAY))
                .andExpect(jsonPath("$.requestRetention").value(UserSettings.DEFAULT_REQUEST_RETENTION))
                .andExpect(jsonPath("$.productionMode").value(UserSettings.DEFAULT_PRODUCTION_MODE))
                .andExpect(jsonPath("$.withTones").value(true));
    }

    @Test
    void patchPersistsAndPartialUpdatesPreserveOtherFields() throws Exception {
        mvc.perform(patch("/api/me/settings")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "productionMode", "CHOICE",
                                "withTones", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productionMode").value("CHOICE"))
                .andExpect(jsonPath("$.withTones").value(false))
                .andExpect(jsonPath("$.drawingHelpLevel").value(UserSettings.DEFAULT_DRAWING_HELP_LEVEL));

        // Second patch only touches retention.
        mvc.perform(patch("/api/me/settings")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("requestRetention", 0.95))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestRetention").value(0.95))
                .andExpect(jsonPath("$.productionMode").value("CHOICE"))
                .andExpect(jsonPath("$.withTones").value(false));

        UserEntity reloaded = users.findById(user.getId()).orElseThrow();
        assertThat(reloaded.getSettings()).containsEntry("productionMode", "CHOICE");
    }

    @Test
    void patchRejectsInvalidValues() throws Exception {
        mvc.perform(patch("/api/me/settings")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("requestRetention", 0.4))))
                .andExpect(status().isBadRequest());

        mvc.perform(patch("/api/me/settings")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("productionMode", "TYPING"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void patchClampsLimits() throws Exception {
        mvc.perform(patch("/api/me/settings")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "newPerDay", 9999,
                                "maxReviewsPerDay", -1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newPerDay").value(200))
                .andExpect(jsonPath("$.maxReviewsPerDay").value(0));
    }
}

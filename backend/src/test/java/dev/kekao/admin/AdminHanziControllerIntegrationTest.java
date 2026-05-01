package dev.kekao.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kekao.AbstractPostgresIntegrationTest;
import dev.kekao.auth.TokenService;
import dev.kekao.hanzi.HanziEntity;
import dev.kekao.hanzi.HanziRepository;
import dev.kekao.hanzi.HanziStatus;
import dev.kekao.hanzi.HanziTranslationEntity;
import dev.kekao.hanzi.HanziTranslationRepository;
import dev.kekao.user.UserEntity;
import dev.kekao.user.UserRepository;
import dev.kekao.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@EnabledIf(value = "dev.kekao.DockerAvailability#isAvailable",
        disabledReason = "Docker daemon is not available; integration tests skipped")
class AdminHanziControllerIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private HanziRepository hanzi;
    @Autowired private HanziTranslationRepository translations;
    @Autowired private UserRepository users;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private TokenService tokens;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        translations.deleteAll();
        hanzi.deleteAll();
        adminToken = tokens.issueAccessToken(createUser("admin-" + System.nanoTime() + "@e.com", UserRole.ROLE_ADMIN));
        userToken = tokens.issueAccessToken(createUser("user-" + System.nanoTime() + "@e.com", UserRole.ROLE_USER));
    }

    private UserEntity createUser(String email, UserRole role) {
        return users.save(UserEntity.builder()
                .email(email.toLowerCase())
                .passwordHash(passwordEncoder.encode("password123"))
                .role(role)
                .build());
    }

    private HanziEntity seedDraft(String character, String pinyin, List<String> meanings) {
        HanziEntity h = hanzi.save(HanziEntity.builder()
                .character(character)
                .pinyin(pinyin)
                .hskLevel((short) 1)
                .status(HanziStatus.DRAFT)
                .build());
        if (meanings != null) {
            translations.save(HanziTranslationEntity.builder()
                    .hanzi(h)
                    .language("en")
                    .meanings(new ArrayList<>(meanings))
                    .primary(true)
                    .build());
        }
        return h;
    }

    @Test
    void listReturnsDraftsForAdmin() throws Exception {
        seedDraft("你", "ni", List.of("you"));
        seedDraft("好", "hao", List.of("good"));

        mvc.perform(get("/api/admin/hanzi?status=DRAFT")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].character").exists())
                .andExpect(jsonPath("$.items[0].meaningsEn").isArray());
    }

    @Test
    void listForbiddenForRegularUser() throws Exception {
        mvc.perform(get("/api/admin/hanzi")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void listUnauthorizedWithoutToken() throws Exception {
        mvc.perform(get("/api/admin/hanzi"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateRewritesPinyinAndMeanings() throws Exception {
        HanziEntity entity = seedDraft("你", "ni", List.of("you"));

        mvc.perform(put("/api/admin/hanzi/" + entity.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "pinyin", "nǐ",
                                "strokeCount", 7,
                                "hskLevel", 1,
                                "meaningsEn", List.of("you (informal)", "you")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pinyin").value("nǐ"))
                .andExpect(jsonPath("$.meaningsEn[0]").value("you (informal)"));

        HanziEntity reloaded = hanzi.findById(entity.getId()).orElseThrow();
        assertThat(reloaded.getPinyin()).isEqualTo("nǐ");
        assertThat(reloaded.getStrokeCount()).isEqualTo((short) 7);
        List<HanziTranslationEntity> ts = translations.findByHanziId(entity.getId());
        assertThat(ts).hasSize(1);
        assertThat(ts.getFirst().getMeanings()).containsExactly("you (informal)", "you");
    }

    @Test
    void updateValidatesBody() throws Exception {
        HanziEntity entity = seedDraft("你", "ni", List.of("you"));

        mvc.perform(put("/api/admin/hanzi/" + entity.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "pinyin", "",
                                "meaningsEn", List.of()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"));
    }

    @Test
    void updateReturns404ForUnknownId() throws Exception {
        mvc.perform(put("/api/admin/hanzi/999999")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "pinyin", "nǐ",
                                "meaningsEn", List.of("you")))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("hanzi_not_found"));
    }

    @Test
    void publishMovesDraftToPublished() throws Exception {
        HanziEntity entity = seedDraft("你", "nǐ", List.of("you"));

        mvc.perform(post("/api/admin/hanzi/" + entity.getId() + "/publish")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        assertThat(hanzi.findById(entity.getId()).orElseThrow().getStatus())
                .isEqualTo(HanziStatus.PUBLISHED);
    }

    @Test
    void publishRejectsEntriesWithoutMeanings() throws Exception {
        HanziEntity entity = seedDraft("好", "hǎo", null);

        mvc.perform(post("/api/admin/hanzi/" + entity.getId() + "/publish")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("hanzi_not_publishable"));
    }

    @Test
    void publishForbiddenForRegularUser() throws Exception {
        HanziEntity entity = seedDraft("你", "nǐ", List.of("you"));

        mvc.perform(post("/api/admin/hanzi/" + entity.getId() + "/publish")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }
}

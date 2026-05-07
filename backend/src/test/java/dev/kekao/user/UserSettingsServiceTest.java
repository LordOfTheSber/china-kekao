package dev.kekao.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserSettingsServiceTest {

    @Mock private UserRepository users;
    @InjectMocks private UserSettingsService service;

    private UserEntity user;

    @BeforeEach
    void setUp() {
        user = UserEntity.builder()
                .id(7L)
                .email("a@b")
                .passwordHash("x")
                .role(UserRole.ROLE_USER)
                .settings(new HashMap<>())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        when(users.findById(7L)).thenReturn(Optional.of(user));
        when(users.save(any(UserEntity.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void getReturnsDefaultsWhenSettingsMapIsEmpty() {
        UserSettings settings = service.get(7L);
        assertThat(settings.newPerDay()).isEqualTo(UserSettings.DEFAULT_NEW_PER_DAY);
        assertThat(settings.maxReviewsPerDay()).isEqualTo(UserSettings.DEFAULT_MAX_REVIEWS_PER_DAY);
        assertThat(settings.requestRetention()).isEqualTo(UserSettings.DEFAULT_REQUEST_RETENTION);
        assertThat(settings.productionMode()).isEqualTo(UserSettings.DEFAULT_PRODUCTION_MODE);
        assertThat(settings.drawingHelpLevel()).isEqualTo(UserSettings.DEFAULT_DRAWING_HELP_LEVEL);
        assertThat(settings.withTones()).isEqualTo(UserSettings.DEFAULT_WITH_TONES);
    }

    @Test
    void updatePersistsValidPatch() {
        UserSettings result = service.update(7L,
                new UserSettings(15, 150, 0.85, "CHOICE", "EASY", false));

        assertThat(result.newPerDay()).isEqualTo(15);
        assertThat(result.maxReviewsPerDay()).isEqualTo(150);
        assertThat(result.requestRetention()).isEqualTo(0.85);
        assertThat(result.productionMode()).isEqualTo("CHOICE");
        assertThat(result.drawingHelpLevel()).isEqualTo("EASY");
        assertThat(result.withTones()).isFalse();

        Map<String, Object> stored = user.getSettings();
        assertThat(stored).containsEntry(UserSettingsService.KEY_NEW_PER_DAY, 15);
        assertThat(stored).containsEntry(UserSettingsService.KEY_PRODUCTION_MODE, "CHOICE");
        assertThat(stored).containsEntry(UserSettingsService.KEY_WITH_TONES, false);
    }

    @Test
    void updateClampsLimitsToTheirAllowedRange() {
        UserSettings result = service.update(7L,
                new UserSettings(-10, 99999, null, null, null, null));
        assertThat(result.newPerDay()).isEqualTo(0);
        assertThat(result.maxReviewsPerDay()).isEqualTo(2000);
    }

    @Test
    void updateRejectsInvalidProductionMode() {
        assertThatThrownBy(() -> service.update(7L,
                new UserSettings(null, null, null, "TYPING", null, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateRejectsInvalidHelpLevel() {
        assertThatThrownBy(() -> service.update(7L,
                new UserSettings(null, null, null, null, "ULTRA", null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateRejectsRetentionOutOfRange() {
        assertThatThrownBy(() -> service.update(7L,
                new UserSettings(null, null, 0.5, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getRecognisesNumericAndStringValues() {
        Map<String, Object> raw = new HashMap<>();
        raw.put(UserSettingsService.KEY_NEW_PER_DAY, "8");
        raw.put(UserSettingsService.KEY_REQUEST_RETENTION, "0.92");
        raw.put(UserSettingsService.KEY_WITH_TONES, "false");
        raw.put(UserSettingsService.KEY_PRODUCTION_MODE, "drawing");
        user.setSettings(raw);

        UserSettings settings = service.get(7L);
        assertThat(settings.newPerDay()).isEqualTo(8);
        assertThat(settings.requestRetention()).isEqualTo(0.92);
        assertThat(settings.withTones()).isFalse();
        assertThat(settings.productionMode()).isEqualTo("DRAWING");
    }

    @Test
    void updatePreservesUnsetFields() {
        service.update(7L, new UserSettings(30, null, null, null, null, null));
        UserSettings result = service.update(7L,
                new UserSettings(null, null, 0.95, null, null, null));
        assertThat(result.newPerDay()).isEqualTo(30);
        assertThat(result.requestRetention()).isEqualTo(0.95);
    }

    @Test
    void getThrowsWhenUserMissing() {
        when(users.findById(404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(404L))
                .isInstanceOf(java.util.NoSuchElementException.class);
    }
}

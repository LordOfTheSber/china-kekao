package dev.kekao.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

@Service
public class UserSettingsService {

    public static final String KEY_NEW_PER_DAY = "newPerDay";
    public static final String KEY_MAX_REVIEWS_PER_DAY = "maxReviewsPerDay";
    public static final String KEY_REQUEST_RETENTION = "requestRetention";
    public static final String KEY_PRODUCTION_MODE = "productionMode";
    public static final String KEY_DRAWING_HELP_LEVEL = "drawingHelpLevel";
    public static final String KEY_WITH_TONES = "withTones";

    private static final Set<String> PRODUCTION_MODES = Set.of("DRAWING", "CHOICE");
    private static final Set<String> HELP_LEVELS = Set.of("STRICT", "NORMAL", "EASY");

    private final UserRepository users;

    @Autowired
    public UserSettingsService(UserRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public UserSettings get(Long userId) {
        UserEntity user = users.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));
        Map<String, Object> s = user.getSettings() == null ? Map.of() : user.getSettings();
        return new UserSettings(
                readInt(s, KEY_NEW_PER_DAY, UserSettings.DEFAULT_NEW_PER_DAY),
                readInt(s, KEY_MAX_REVIEWS_PER_DAY, UserSettings.DEFAULT_MAX_REVIEWS_PER_DAY),
                readDouble(s, KEY_REQUEST_RETENTION, UserSettings.DEFAULT_REQUEST_RETENTION),
                readEnum(s, KEY_PRODUCTION_MODE, PRODUCTION_MODES, UserSettings.DEFAULT_PRODUCTION_MODE),
                readEnum(s, KEY_DRAWING_HELP_LEVEL, HELP_LEVELS, UserSettings.DEFAULT_DRAWING_HELP_LEVEL),
                readBoolean(s, KEY_WITH_TONES, UserSettings.DEFAULT_WITH_TONES));
    }

    @Transactional
    public UserSettings update(Long userId, UserSettings patch) {
        UserEntity user = users.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));
        Map<String, Object> s = user.getSettings() == null ? new HashMap<>() : new HashMap<>(user.getSettings());
        if (patch.newPerDay() != null) {
            s.put(KEY_NEW_PER_DAY, clampInt(patch.newPerDay(), 0, 200));
        }
        if (patch.maxReviewsPerDay() != null) {
            s.put(KEY_MAX_REVIEWS_PER_DAY, clampInt(patch.maxReviewsPerDay(), 0, 2000));
        }
        if (patch.requestRetention() != null) {
            double r = patch.requestRetention();
            if (r < 0.7 || r > 0.99) {
                throw new IllegalArgumentException("requestRetention must be between 0.7 and 0.99");
            }
            s.put(KEY_REQUEST_RETENTION, r);
        }
        if (patch.productionMode() != null) {
            String pm = patch.productionMode().toUpperCase();
            if (!PRODUCTION_MODES.contains(pm)) {
                throw new IllegalArgumentException("productionMode must be one of " + PRODUCTION_MODES);
            }
            s.put(KEY_PRODUCTION_MODE, pm);
        }
        if (patch.drawingHelpLevel() != null) {
            String hl = patch.drawingHelpLevel().toUpperCase();
            if (!HELP_LEVELS.contains(hl)) {
                throw new IllegalArgumentException("drawingHelpLevel must be one of " + HELP_LEVELS);
            }
            s.put(KEY_DRAWING_HELP_LEVEL, hl);
        }
        if (patch.withTones() != null) {
            s.put(KEY_WITH_TONES, patch.withTones());
        }
        user.setSettings(s);
        users.save(user);
        return get(userId);
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static Integer readInt(Map<String, Object> s, String key, int defaultValue) {
        Object raw = s.get(key);
        if (raw instanceof Number n) return n.intValue();
        if (raw instanceof String str) {
            try { return Integer.parseInt(str.trim()); } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }

    private static Double readDouble(Map<String, Object> s, String key, double defaultValue) {
        Object raw = s.get(key);
        if (raw instanceof Number n) return n.doubleValue();
        if (raw instanceof String str) {
            try { return Double.parseDouble(str.trim()); } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }

    private static Boolean readBoolean(Map<String, Object> s, String key, boolean defaultValue) {
        Object raw = s.get(key);
        if (raw instanceof Boolean b) return b;
        if (raw instanceof String str) return Boolean.parseBoolean(str.trim());
        return defaultValue;
    }

    private static String readEnum(Map<String, Object> s, String key, Set<String> allowed, String defaultValue) {
        Object raw = s.get(key);
        if (raw instanceof String str) {
            String upper = str.toUpperCase();
            if (allowed.contains(upper)) return upper;
        }
        return defaultValue;
    }
}

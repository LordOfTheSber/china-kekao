package dev.kekao.user;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserSettings(
        Integer newPerDay,
        Integer maxReviewsPerDay,
        Double requestRetention,
        String productionMode,
        String drawingHelpLevel,
        Boolean withTones
) {
    public static final int DEFAULT_NEW_PER_DAY = 20;
    public static final int DEFAULT_MAX_REVIEWS_PER_DAY = 200;
    public static final double DEFAULT_REQUEST_RETENTION = 0.9;
    public static final String DEFAULT_PRODUCTION_MODE = "DRAWING";
    public static final String DEFAULT_DRAWING_HELP_LEVEL = "NORMAL";
    public static final boolean DEFAULT_WITH_TONES = true;

    public static UserSettings defaults() {
        return new UserSettings(
                DEFAULT_NEW_PER_DAY,
                DEFAULT_MAX_REVIEWS_PER_DAY,
                DEFAULT_REQUEST_RETENTION,
                DEFAULT_PRODUCTION_MODE,
                DEFAULT_DRAWING_HELP_LEVEL,
                DEFAULT_WITH_TONES);
    }
}

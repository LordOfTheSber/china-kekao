package dev.kekao.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "kekao.auth")
public record AuthProperties(
        Duration accessTokenTtl,
        Duration refreshTokenTtl,
        String issuer,
        String privateKey,
        String publicKey,
        RateLimit rateLimit
) {
    public record RateLimit(int capacity, Duration window) {}
}

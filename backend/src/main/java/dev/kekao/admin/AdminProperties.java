package dev.kekao.admin;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kekao.admin")
public record AdminProperties(String bootstrapEmail) {
}

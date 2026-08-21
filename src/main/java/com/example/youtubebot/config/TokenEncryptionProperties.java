package com.example.youtubebot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("youtube.token-encryption")
public record TokenEncryptionProperties(String key, int keyVersion) {

    public TokenEncryptionProperties {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("YOUTUBE_TOKEN_ENCRYPTION_KEY is required");
        }
        if (keyVersion <= 0) {
            throw new IllegalArgumentException("Token encryption key version must be positive");
        }
    }
}

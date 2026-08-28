package com.example.youtubebot.oauth;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

@ConfigurationProperties("google.oauth")
public record GoogleOAuthProperties(
        String clientId,
        String clientSecret,
        URI redirectUri,
        URI authorizationUri,
        URI tokenUri,
        URI revokeUri,
        URI youtubeApiUri) {

    public static final String YOUTUBE_SCOPE =
            "https://www.googleapis.com/auth/youtube.force-ssl";

    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank()
                && clientSecret != null && !clientSecret.isBlank();
    }

    public void requireConfigured() {
        if (!isConfigured()) {
            throw new GoogleOAuthException(
                    GoogleOAuthErrorCode.OAUTH_NOT_CONFIGURED,
                    "Google OAuth client ID and client secret are required");
        }
    }
}

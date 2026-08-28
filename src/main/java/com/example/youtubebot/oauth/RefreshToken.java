package com.example.youtubebot.oauth;

public record RefreshToken(String value) {

    public RefreshToken {
        if (value == null || value.isBlank()) {
            throw new GoogleOAuthException(
                    GoogleOAuthErrorCode.MISSING_REFRESH_TOKEN,
                    "Google did not return a refresh token; reconnect with consent");
        }
    }

    @Override
    public String toString() {
        return "RefreshToken[REDACTED]";
    }
}

package com.example.youtubebot.oauth;

import java.util.Objects;

public record GoogleTokenGrant(
        String accessToken,
        RefreshToken refreshToken,
        GrantedScopes grantedScopes) {

    public GoogleTokenGrant {
        if (accessToken == null || accessToken.isBlank()) {
            throw new GoogleOAuthException(
                    GoogleOAuthErrorCode.MISSING_ACCESS_TOKEN,
                    "Google did not return an access token");
        }
        Objects.requireNonNull(refreshToken, "Refresh token is required");
        Objects.requireNonNull(grantedScopes, "Granted scopes are required");
    }
}

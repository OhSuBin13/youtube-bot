package com.example.youtubebot.oauth;

public interface GoogleOAuthGateway {

    GoogleTokenGrant exchangeAuthorizationCode(String code, String codeVerifier);

    YouTubeChannelIdentity findAuthenticatedChannel(String accessToken);

    void revoke(RefreshToken refreshToken);
}

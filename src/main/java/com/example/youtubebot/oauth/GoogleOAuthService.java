package com.example.youtubebot.oauth;

import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

@Service
public class GoogleOAuthService {

    private static final Duration FLOW_TTL = Duration.ofMinutes(10);

    private final GoogleOAuthProperties properties;
    private final GoogleOAuthGateway gateway;
    private final OAuthTokenStore tokenStore;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    public GoogleOAuthService(
            GoogleOAuthProperties properties,
            GoogleOAuthGateway gateway,
            OAuthTokenStore tokenStore,
            Clock clock) {
        this.properties = properties;
        this.gateway = gateway;
        this.tokenStore = tokenStore;
        this.clock = clock;
    }

    public OAuthAuthorization beginConnection() {
        properties.requireConfigured();
        if (tokenStore.exists()) {
            throw new GoogleOAuthException(
                    GoogleOAuthErrorCode.ALREADY_CONNECTED,
                    "Disconnect the current YouTube channel before connecting another one");
        }

        String state = randomUrlSafeValue(32);
        String codeVerifier = randomUrlSafeValue(64);
        String codeChallenge = sha256UrlSafe(codeVerifier);
        OAuthFlowState flowState = new OAuthFlowState(state, codeVerifier, clock.instant());
        URI authorizationUri = UriComponentsBuilder.fromUri(properties.authorizationUri())
                .queryParam("client_id", properties.clientId())
                .queryParam("redirect_uri", properties.redirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", GoogleOAuthProperties.YOUTUBE_SCOPE)
                .queryParam("access_type", "offline")
                .queryParam("prompt", "consent")
                .queryParam("state", state)
                .queryParam("code_challenge", codeChallenge)
                .queryParam("code_challenge_method", "S256")
                .build()
                .encode()
                .toUri();
        return new OAuthAuthorization(authorizationUri, flowState);
    }

    public YouTubeChannelIdentity completeConnection(
            String code,
            String returnedState,
            OAuthFlowState flowState) {
        properties.requireConfigured();
        if (code == null || code.isBlank()) {
            throw new GoogleOAuthException(
                    GoogleOAuthErrorCode.AUTHORIZATION_CODE_MISSING,
                    "Authorization code is missing");
        }
        validateFlowState(returnedState, flowState);
        if (tokenStore.exists()) {
            throw new GoogleOAuthException(
                    GoogleOAuthErrorCode.ALREADY_CONNECTED,
                    "Disconnect the current YouTube channel before connecting another one");
        }

        GoogleTokenGrant grant = gateway.exchangeAuthorizationCode(code, flowState.codeVerifier());
        try {
            YouTubeChannelIdentity channel = gateway.findAuthenticatedChannel(grant.accessToken());
            tokenStore.create(new OAuthConnectionInput(
                    grant.refreshToken(),
                    grant.grantedScopes(),
                    channel,
                    clock.instant()));
            return channel;
        } catch (RuntimeException exception) {
            revokeAfterFailedConnection(grant.refreshToken(), exception);
            throw exception;
        }
    }

    public void validateAuthorizationError(String returnedState, OAuthFlowState flowState) {
        validateFlowState(returnedState, flowState);
    }

    public void disconnect() {
        Optional<OAuthConnectionCredentials> connection = tokenStore.find();
        if (connection.isEmpty()) {
            return;
        }
        gateway.revoke(connection.orElseThrow().refreshToken());
        tokenStore.delete();
    }

    public OAuthConnectionStatus status() {
        boolean configured = properties.isConfigured();
        return tokenStore.findConnectionInfo()
                .map(connection -> OAuthConnectionStatus.connected(configured, connection))
                .orElseGet(() -> OAuthConnectionStatus.disconnected(configured));
    }

    public YouTubeChannelIdentity requireConnectedChannel() {
        OAuthConnectionInfo connection = tokenStore.findConnectionInfo()
                .orElseThrow(() -> new GoogleOAuthException(
                        GoogleOAuthErrorCode.OAUTH_NOT_CONNECTED,
                        "Connect a YouTube channel before continuing"));
        return connection.channel();
    }

    private void validateFlowState(String returnedState, OAuthFlowState flowState) {
        if (flowState == null) {
            throw new GoogleOAuthException(
                    GoogleOAuthErrorCode.OAUTH_SESSION_MISSING,
                    "OAuth session state is missing");
        }
        if (returnedState == null || !constantTimeEquals(flowState.state(), returnedState)) {
            throw new GoogleOAuthException(
                    GoogleOAuthErrorCode.STATE_MISMATCH,
                    "OAuth state validation failed");
        }
        Instant now = clock.instant();
        if (flowState.initiatedAt() == null
                || flowState.initiatedAt().isAfter(now.plusSeconds(5))
                || flowState.initiatedAt().plus(FLOW_TTL).isBefore(now)) {
            throw new GoogleOAuthException(
                    GoogleOAuthErrorCode.OAUTH_SESSION_EXPIRED,
                    "OAuth session state has expired");
        }
        if (flowState.codeVerifier() == null || flowState.codeVerifier().isBlank()) {
            throw new GoogleOAuthException(
                    GoogleOAuthErrorCode.PKCE_VERIFIER_MISSING,
                    "PKCE verifier is missing");
        }
    }

    private void revokeAfterFailedConnection(RefreshToken refreshToken, RuntimeException original) {
        try {
            gateway.revoke(refreshToken);
        } catch (RuntimeException revokeFailure) {
            original.addSuppressed(revokeFailure);
        }
    }

    private String randomUrlSafeValue(int byteLength) {
        byte[] bytes = new byte[byteLength];
        secureRandom.nextBytes(bytes);
        try {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } finally {
            java.util.Arrays.fill(bytes, (byte) 0);
        }
    }

    private String sha256UrlSafe(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.US_ASCII));
            try {
                return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
            } finally {
                java.util.Arrays.fill(digest, (byte) 0);
            }
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }
}

package com.example.youtubebot.oauth;

import java.util.Arrays;
import java.util.Optional;

public enum GoogleOAuthErrorCode {

    OAUTH_NOT_CONFIGURED("oauth_not_configured"),
    ALREADY_CONNECTED("already_connected"),
    AUTHORIZATION_DENIED("authorization_denied"),
    OAUTH_SESSION_MISSING("oauth_session_missing"),
    OAUTH_SESSION_EXPIRED("oauth_session_expired"),
    STATE_MISMATCH("state_mismatch"),
    AUTHORIZATION_CODE_MISSING("authorization_code_missing"),
    PKCE_VERIFIER_MISSING("pkce_verifier_missing"),
    TOKEN_EXCHANGE_FAILED("token_exchange_failed"),
    MISSING_ACCESS_TOKEN("missing_access_token"),
    MISSING_REFRESH_TOKEN("missing_refresh_token"),
    REQUIRED_SCOPE_MISSING("required_scope_missing"),
    CHANNEL_LOOKUP_FAILED("channel_lookup_failed"),
    CHANNEL_NOT_UNIQUE("channel_not_unique"),
    INVALID_CHANNEL("invalid_channel"),
    TOKEN_REVOKE_FAILED("token_revoke_failed"),
    OAUTH_NOT_CONNECTED("oauth_not_connected");

    private final String value;

    GoogleOAuthErrorCode(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static Optional<GoogleOAuthErrorCode> fromValue(String value) {
        if (value == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(errorCode -> errorCode.value.equals(value))
                .findFirst();
    }
}

package com.example.youtubebot.oauth;

import java.util.Objects;

public class GoogleOAuthException extends RuntimeException {

    private final GoogleOAuthErrorCode errorCode;

    public GoogleOAuthException(GoogleOAuthErrorCode errorCode, String message) {
        super(message);
        this.errorCode = Objects.requireNonNull(errorCode, "OAuth error code is required");
    }

    public GoogleOAuthErrorCode getErrorCode() {
        return errorCode;
    }
}

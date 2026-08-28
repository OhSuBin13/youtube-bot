package com.example.youtubebot.oauth;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GoogleOAuthErrorCodeTest {

    @Test
    void externalValuesAreUniqueAndRoundTripToTheirEnum() {
        Set<String> values = Arrays.stream(GoogleOAuthErrorCode.values())
                .map(GoogleOAuthErrorCode::value)
                .collect(Collectors.toSet());

        assertEquals(GoogleOAuthErrorCode.values().length, values.size());
        for (GoogleOAuthErrorCode errorCode : GoogleOAuthErrorCode.values()) {
            assertSame(
                    errorCode,
                    GoogleOAuthErrorCode.fromValue(errorCode.value()).orElseThrow());
        }
        assertTrue(GoogleOAuthErrorCode.fromValue("unknown").isEmpty());
    }
}

package com.example.youtubebot.oauth;

import java.util.Set;

public record GrantedScopes(Set<String> values) {

    public GrantedScopes {
        if (values == null
                || values.isEmpty()
                || values.stream().anyMatch(scope ->
                        scope == null
                                || scope.isBlank()
                                || scope.chars().anyMatch(Character::isWhitespace))
                || !values.contains(GoogleOAuthProperties.YOUTUBE_SCOPE)) {
            throw new GoogleOAuthException(
                    GoogleOAuthErrorCode.REQUIRED_SCOPE_MISSING,
                    "The required YouTube scope was not granted");
        }
        values = Set.copyOf(values);
    }
}

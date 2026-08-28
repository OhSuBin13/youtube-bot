package com.example.youtubebot.oauth;

import java.net.URI;

public record OAuthAuthorization(URI authorizationUri, OAuthFlowState flowState) {
}

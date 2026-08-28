package com.example.youtubebot.oauth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class GoogleOAuthHttpClient implements GoogleOAuthGateway {

    private final GoogleOAuthProperties properties;
    private final RestClient restClient;

    public GoogleOAuthHttpClient(
            GoogleOAuthProperties properties,
            @Qualifier(GoogleOAuthConfiguration.GOOGLE_OAUTH_REST_CLIENT)
            RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    @Override
    public GoogleTokenGrant exchangeAuthorizationCode(String code, String codeVerifier) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("code", code);
        form.add("client_id", properties.clientId());
        form.add("client_secret", properties.clientSecret());
        form.add("redirect_uri", properties.redirectUri().toString());
        form.add("grant_type", "authorization_code");
        form.add("code_verifier", codeVerifier);

        try {
            TokenResponse response = restClient.post()
                    .uri(properties.tokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .onStatus(status -> status.isError(), (request, failedResponse) -> {
                        throw new GoogleOAuthException(
                                GoogleOAuthErrorCode.TOKEN_EXCHANGE_FAILED,
                                "Google OAuth token exchange failed");
                    })
                    .body(TokenResponse.class);
            if (response == null) {
                throw new GoogleOAuthException(
                        GoogleOAuthErrorCode.TOKEN_EXCHANGE_FAILED,
                        "Google OAuth token exchange returned an empty response");
            }
            return new GoogleTokenGrant(
                    response.accessToken(),
                    new RefreshToken(response.refreshToken()),
                    new GrantedScopes(parseScopes(response.scope())));
        } catch (GoogleOAuthException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new GoogleOAuthException(
                    GoogleOAuthErrorCode.TOKEN_EXCHANGE_FAILED,
                    "Google OAuth token exchange could not be completed");
        }
    }

    @Override
    public YouTubeChannelIdentity findAuthenticatedChannel(String accessToken) {
        URI uri = UriComponentsBuilder.fromUri(properties.youtubeApiUri())
                .path("/channels")
                .queryParam("part", "snippet")
                .queryParam("mine", "true")
                .build()
                .encode()
                .toUri();

        try {
            ChannelListResponse response = restClient.get()
                    .uri(uri)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .onStatus(status -> status.isError(), (request, failedResponse) -> {
                        throw new GoogleOAuthException(
                                GoogleOAuthErrorCode.CHANNEL_LOOKUP_FAILED,
                                "The authenticated YouTube channel could not be read");
                    })
                    .body(ChannelListResponse.class);
            List<ChannelItem> items = response == null || response.items() == null
                    ? List.of()
                    : response.items();
            if (items.size() != 1) {
                throw new GoogleOAuthException(
                        GoogleOAuthErrorCode.CHANNEL_NOT_UNIQUE,
                        "Exactly one authenticated YouTube channel is required");
            }
            ChannelItem channel = items.getFirst();
            String title = channel.snippet() == null ? null : channel.snippet().title();
            return new YouTubeChannelIdentity(channel.id(), title);
        } catch (GoogleOAuthException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new GoogleOAuthException(
                    GoogleOAuthErrorCode.CHANNEL_LOOKUP_FAILED,
                    "The authenticated YouTube channel could not be read");
        }
    }

    @Override
    public void revoke(RefreshToken refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("token", refreshToken.value());

        try {
            restClient.post()
                    .uri(properties.revokeUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .onStatus(status -> status.value() == 400, (request, response) -> {
                        // An invalid or already-revoked token is disconnected locally as well.
                    })
                    .onStatus(status -> status.isError(), (request, response) -> {
                        throw new GoogleOAuthException(
                                GoogleOAuthErrorCode.TOKEN_REVOKE_FAILED,
                                "Google OAuth token revocation failed");
                    })
                    .toBodilessEntity();
        } catch (GoogleOAuthException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new GoogleOAuthException(
                    GoogleOAuthErrorCode.TOKEN_REVOKE_FAILED,
                    "Google OAuth token revocation could not be completed");
        }
    }

    private Set<String> parseScopes(String scope) {
        if (scope == null || scope.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(scope.trim().split("\\s+"))
                .collect(Collectors.toUnmodifiableSet());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("refresh_token") String refreshToken,
            String scope) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ChannelListResponse(List<ChannelItem> items) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ChannelItem(String id, ChannelSnippet snippet) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ChannelSnippet(String title) {
    }
}

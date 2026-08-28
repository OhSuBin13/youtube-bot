package com.example.youtubebot.oauth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GoogleOAuthHttpClientTest {

    private static final URI TOKEN_URI = URI.create("https://oauth.test/token");
    private static final URI REVOKE_URI = URI.create("https://oauth.test/revoke");

    private MockRestServiceServer server;
    private GoogleOAuthHttpClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        GoogleOAuthProperties properties = new GoogleOAuthProperties(
                "test-client-id",
                "test-client-secret",
                URI.create("http://127.0.0.1/oauth/callback"),
                URI.create("https://accounts.test/o/oauth2/v2/auth"),
                TOKEN_URI,
                REVOKE_URI,
                URI.create("https://youtube.test/youtube/v3"));
        client = new GoogleOAuthHttpClient(properties, builder.build());
    }

    @Test
    void exchangesAuthorizationCodeWithPkceFormFields() {
        MultiValueMap<String, String> expectedForm = new LinkedMultiValueMap<>();
        expectedForm.add("code", "authorization-code");
        expectedForm.add("client_id", "test-client-id");
        expectedForm.add("client_secret", "test-client-secret");
        expectedForm.add("redirect_uri", "http://127.0.0.1/oauth/callback");
        expectedForm.add("grant_type", "authorization_code");
        expectedForm.add("code_verifier", "pkce-verifier");
        server.expect(once(), requestTo(TOKEN_URI))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().formData(expectedForm))
                .andRespond(withSuccess("""
                        {
                          "access_token": "access-token",
                          "refresh_token": "refresh-token",
                          "scope": "https://www.googleapis.com/auth/youtube.force-ssl"
                        }
                        """, MediaType.APPLICATION_JSON));

        GoogleTokenGrant grant = client.exchangeAuthorizationCode(
                "authorization-code", "pkce-verifier");

        assertEquals("access-token", grant.accessToken());
        assertEquals("refresh-token", grant.refreshToken().value());
        assertEquals(
                Set.of(GoogleOAuthProperties.YOUTUBE_SCOPE),
                grant.grantedScopes().values());
        server.verify();
    }

    @Test
    void readsTheAuthenticatedYouTubeChannelWithMineTrue() {
        server.expect(once(), requestTo(
                        "https://youtube.test/youtube/v3/channels?part=snippet&mine=true"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
                .andRespond(withSuccess("""
                        {
                          "items": [
                            {"id": "UC_FIXED_AUTHOR", "snippet": {"title": "Fixed Author"}}
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        YouTubeChannelIdentity channel = client.findAuthenticatedChannel("access-token");

        assertEquals(new YouTubeChannelIdentity("UC_FIXED_AUTHOR", "Fixed Author"), channel);
        server.verify();
    }

    @Test
    void treatsAlreadyInvalidTokenAsRevoked() {
        MultiValueMap<String, String> expectedForm = new LinkedMultiValueMap<>();
        expectedForm.add("token", "refresh-token");
        server.expect(once(), requestTo(REVOKE_URI))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().formData(expectedForm))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        client.revoke(new RefreshToken("refresh-token"));

        server.verify();
    }
}

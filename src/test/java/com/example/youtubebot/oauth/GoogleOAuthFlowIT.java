package com.example.youtubebot.oauth;

import com.example.youtubebot.support.PostgreSqlIntegrationTest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "google.oauth.client-id=test-client-id",
                "google.oauth.client-secret=test-client-secret",
                "google.oauth.redirect-uri=http://127.0.0.1/oauth/callback",
                "google.oauth.authorization-uri=https://accounts.test/o/oauth2/v2/auth",
                "google.oauth.token-uri=https://oauth.test/token",
                "google.oauth.revoke-uri=https://oauth.test/revoke",
                "google.oauth.youtube-api-uri=https://youtube.test/youtube/v3"
        })
@AutoConfigureMockMvc
@Import(GoogleOAuthFlowIT.StubConfiguration.class)
class GoogleOAuthFlowIT extends PostgreSqlIntegrationTest {

    @Autowired
    private GoogleOAuthService oauthService;

    @Autowired
    private OAuthTokenStore tokenStore;

    @Autowired
    private StubGoogleOAuthGateway googleGateway;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void resetGoogleGateway() {
        googleGateway.reset();
    }

    @Test
    void callbackPersistsEncryptedRefreshTokenAndFixesAuthenticatedChannel() throws Exception {
        MvcResult connectResult = mockMvc.perform(get("/oauth/connect"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        String googleLocation = connectResult.getResponse().getRedirectedUrl();
        assertNotNull(googleLocation);

        HttpSession session = connectResult.getRequest().getSession(false);
        assertNotNull(session);
        OAuthFlowState flowState = (OAuthFlowState) session.getAttribute(
                GoogleOAuthController.FLOW_SESSION_ATTRIBUTE);
        assertNotNull(flowState);

        UriComponents authorization = UriComponentsBuilder.fromUriString(googleLocation).build();
        assertEquals("test-client-id", authorization.getQueryParams().getFirst("client_id"));
        assertEquals("http://127.0.0.1/oauth/callback",
                authorization.getQueryParams().getFirst("redirect_uri"));
        assertEquals(GoogleOAuthProperties.YOUTUBE_SCOPE,
                authorization.getQueryParams().getFirst("scope"));
        assertEquals("offline", authorization.getQueryParams().getFirst("access_type"));
        assertEquals("consent", authorization.getQueryParams().getFirst("prompt"));
        assertFalse(authorization.getQueryParams().containsKey("include_granted_scopes"));
        assertEquals("S256", authorization.getQueryParams().getFirst("code_challenge_method"));
        assertEquals(flowState.state(), authorization.getQueryParams().getFirst("state"));
        assertEquals(pkceChallenge(flowState.codeVerifier()),
                authorization.getQueryParams().getFirst("code_challenge"));

        mockMvc.perform(get("/oauth/callback")
                        .session((org.springframework.mock.web.MockHttpSession) session)
                        .param("code", "authorization-code")
                        .param("state", flowState.state()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/oauth?result=connected"));

        assertNull(session.getAttribute(GoogleOAuthController.FLOW_SESSION_ATTRIBUTE));
        assertEquals("authorization-code", googleGateway.exchangedCode);
        assertEquals(flowState.codeVerifier(), googleGateway.exchangedVerifier);

        OAuthConnectionStatus status = oauthService.status();
        assertTrue(status.connected());
        assertEquals("UC_FIXED_AUTHOR", status.channelId());
        assertEquals("Fixed Author", status.channelName());
        assertEquals(new YouTubeChannelIdentity("UC_FIXED_AUTHOR", "Fixed Author"),
                oauthService.requireConnectedChannel());

        OAuthConnectionCredentials restored = tokenStore.find().orElseThrow();
        assertEquals("1//refresh-token", restored.refreshToken().value());
        assertEquals(
                Set.of(GoogleOAuthProperties.YOUTUBE_SCOPE),
                restored.grantedScopes().values());
        byte[] ciphertext = jdbcTemplate.queryForObject(
                "SELECT refresh_token_ciphertext FROM oauth_connection WHERE id = 1",
                byte[].class);
        assertNotNull(ciphertext);
        assertNotEquals("1//refresh-token", new String(ciphertext, StandardCharsets.UTF_8));

        mockMvc.perform(get("/oauth"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Fixed Author")));
        mockMvc.perform(get("/oauth/connect"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/oauth?error=already_connected"));
    }

    @Test
    void callbackRejectsWrongStateBeforeCallingGoogleAndConsumesSessionState() throws Exception {
        MvcResult connectResult = mockMvc.perform(get("/oauth/connect")).andReturn();
        org.springframework.mock.web.MockHttpSession session =
                (org.springframework.mock.web.MockHttpSession) connectResult.getRequest().getSession(false);

        mockMvc.perform(get("/oauth/callback")
                        .session(session)
                        .param("code", "authorization-code")
                        .param("state", "attacker-state"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/oauth?error=state_mismatch"));

        assertNull(session.getAttribute(GoogleOAuthController.FLOW_SESSION_ATTRIBUTE));
        assertEquals(0, googleGateway.exchangeCount);
        assertFalse(tokenStore.exists());

        mockMvc.perform(get("/oauth/callback")
                        .session(session)
                        .param("code", "authorization-code")
                        .param("state", "attacker-state"))
                .andExpect(redirectedUrl("/oauth?error=oauth_session_missing"));
    }

    @Test
    void errorPageHandlesKnownAndUnknownCodesWithoutChangingTheirMeaning() throws Exception {
        mockMvc.perform(get("/oauth")
                        .param("error", GoogleOAuthErrorCode.OAUTH_NOT_CONNECTED.value()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "YouTube 작성 채널을 먼저 연결해주세요.")));

        String unknownErrorPage = mockMvc.perform(get("/oauth")
                        .param("error", "future_unknown_error"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        assertTrue(unknownErrorPage.contains("알 수 없는 오류가 발생했습니다."));
        assertFalse(unknownErrorPage.contains("작성 채널 정보를 조회하지 못했습니다."));
    }

    @Test
    void failedChannelLookupRevokesNewGrantAndDoesNotSaveConnection() {
        OAuthAuthorization authorization = oauthService.beginConnection();
        googleGateway.channelFailure = new GoogleOAuthException(
                GoogleOAuthErrorCode.CHANNEL_LOOKUP_FAILED,
                "simulated channel failure");

        GoogleOAuthException exception = assertThrows(GoogleOAuthException.class, () ->
                oauthService.completeConnection(
                        "authorization-code",
                        authorization.flowState().state(),
                        authorization.flowState()));

        assertEquals(GoogleOAuthErrorCode.CHANNEL_LOOKUP_FAILED, exception.getErrorCode());
        assertEquals(List.of("1//refresh-token"), googleGateway.revokedTokens);
        assertFalse(tokenStore.exists());
    }

    @Test
    void disconnectRequiresCsrfAndDeletesOnlyAfterGoogleRevocationSucceeds() throws Exception {
        connectDirectly();

        mockMvc.perform(post("/oauth/disconnect"))
                .andExpect(status().isForbidden());
        assertTrue(tokenStore.exists());

        googleGateway.revokeFailure = new GoogleOAuthException(
                GoogleOAuthErrorCode.TOKEN_REVOKE_FAILED,
                "simulated revoke failure");
        mockMvc.perform(post("/oauth/disconnect").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/oauth?error=token_revoke_failed"));
        assertTrue(tokenStore.exists());

        googleGateway.revokeFailure = null;
        mockMvc.perform(post("/oauth/disconnect").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/oauth?result=disconnected"));
        assertFalse(tokenStore.exists());
        assertEquals(List.of("1//refresh-token"), googleGateway.revokedTokens);
    }

    private void connectDirectly() {
        OAuthAuthorization authorization = oauthService.beginConnection();
        oauthService.completeConnection(
                "authorization-code",
                authorization.flowState().state(),
                authorization.flowState());
    }

    private String pkceChallenge(String verifier) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(verifier.getBytes(StandardCharsets.US_ASCII));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class StubConfiguration {

        @Bean
        @Primary
        StubGoogleOAuthGateway stubGoogleOAuthGateway() {
            return new StubGoogleOAuthGateway();
        }
    }

    static class StubGoogleOAuthGateway implements GoogleOAuthGateway {

        private int exchangeCount;
        private String exchangedCode;
        private String exchangedVerifier;
        private RuntimeException channelFailure;
        private RuntimeException revokeFailure;
        private final List<String> revokedTokens = new ArrayList<>();

        @Override
        public GoogleTokenGrant exchangeAuthorizationCode(String code, String codeVerifier) {
            exchangeCount++;
            exchangedCode = code;
            exchangedVerifier = codeVerifier;
            return new GoogleTokenGrant(
                    "short-lived-access-token",
                    new RefreshToken("1//refresh-token"),
                    new GrantedScopes(Set.of(GoogleOAuthProperties.YOUTUBE_SCOPE)));
        }

        @Override
        public YouTubeChannelIdentity findAuthenticatedChannel(String accessToken) {
            assertEquals("short-lived-access-token", accessToken);
            if (channelFailure != null) {
                throw channelFailure;
            }
            return new YouTubeChannelIdentity("UC_FIXED_AUTHOR", "Fixed Author");
        }

        @Override
        public void revoke(RefreshToken refreshToken) {
            if (revokeFailure != null) {
                throw revokeFailure;
            }
            revokedTokens.add(refreshToken.value());
        }

        void reset() {
            exchangeCount = 0;
            exchangedCode = null;
            exchangedVerifier = null;
            channelFailure = null;
            revokeFailure = null;
            revokedTokens.clear();
        }
    }
}

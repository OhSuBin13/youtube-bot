package com.example.youtubebot.oauth;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class GoogleOAuthController {

    static final String FLOW_SESSION_ATTRIBUTE =
            GoogleOAuthController.class.getName() + ".flow";

    private final GoogleOAuthService oauthService;

    public GoogleOAuthController(GoogleOAuthService oauthService) {
        this.oauthService = oauthService;
    }

    @GetMapping("/")
    String home() {
        return "redirect:/oauth";
    }

    @GetMapping("/oauth")
    String connectionPage(
            @RequestParam(required = false) String result,
            @RequestParam(required = false) String error,
            Model model) {
        model.addAttribute("connection", oauthService.status());
        model.addAttribute("resultMessage", resultMessage(result));
        model.addAttribute("errorMessage", errorMessage(error));
        return "oauth/connection";
    }

    @GetMapping("/oauth/connect")
    String connect(HttpSession session) {
        try {
            OAuthAuthorization authorization = oauthService.beginConnection();
            session.setAttribute(FLOW_SESSION_ATTRIBUTE, authorization.flowState());
            return "redirect:" + authorization.authorizationUri();
        } catch (GoogleOAuthException exception) {
            return errorRedirect(exception.getErrorCode());
        }
    }

    @GetMapping("/oauth/callback")
    String callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            HttpSession session) {
        Object storedFlow = session.getAttribute(FLOW_SESSION_ATTRIBUTE);
        session.removeAttribute(FLOW_SESSION_ATTRIBUTE);
        OAuthFlowState flowState = storedFlow instanceof OAuthFlowState value ? value : null;

        try {
            if (error != null && !error.isBlank()) {
                oauthService.validateAuthorizationError(state, flowState);
                return errorRedirect(GoogleOAuthErrorCode.AUTHORIZATION_DENIED);
            }
            oauthService.completeConnection(code, state, flowState);
            return "redirect:/oauth?result=connected";
        } catch (GoogleOAuthException exception) {
            return errorRedirect(exception.getErrorCode());
        }
    }

    @PostMapping("/oauth/disconnect")
    String disconnect(HttpSession session) {
        session.removeAttribute(FLOW_SESSION_ATTRIBUTE);
        try {
            oauthService.disconnect();
            return "redirect:/oauth?result=disconnected";
        } catch (GoogleOAuthException exception) {
            return errorRedirect(exception.getErrorCode());
        }
    }

    private String errorRedirect(GoogleOAuthErrorCode errorCode) {
        return "redirect:/oauth?error=" + errorCode.value();
    }

    private String resultMessage(String result) {
        if ("connected".equals(result)) {
            return "YouTube 작성 채널을 연결하고 고정했습니다.";
        }
        if ("disconnected".equals(result)) {
            return "Google OAuth 연결을 해제했습니다.";
        }
        return null;
    }

    private String errorMessage(String error) {
        if (error == null) {
            return null;
        }
        return GoogleOAuthErrorCode.fromValue(error)
                .map(this::userMessage)
                .orElse("Google OAuth 요청 처리 중 알 수 없는 오류가 발생했습니다.");
    }

    private String userMessage(GoogleOAuthErrorCode errorCode) {
        return switch (errorCode) {
            case OAUTH_NOT_CONFIGURED -> "Google OAuth 환경 변수를 먼저 설정해주세요.";
            case ALREADY_CONNECTED -> "다른 채널을 연결하려면 현재 채널을 먼저 해제해주세요.";
            case AUTHORIZATION_DENIED -> "Google OAuth 승인이 취소되었습니다.";
            case OAUTH_SESSION_MISSING -> "연결 세션이 없거나 이미 사용되었습니다. 다시 시도해주세요.";
            case OAUTH_SESSION_EXPIRED -> "연결 세션이 만료되었습니다. 다시 시도해주세요.";
            case STATE_MISMATCH -> "OAuth 요청 검증에 실패했습니다. 다시 시도해주세요.";
            case AUTHORIZATION_CODE_MISSING -> "Google 인증 코드를 받지 못했습니다.";
            case PKCE_VERIFIER_MISSING -> "OAuth 요청 검증 정보가 없습니다. 다시 시도해주세요.";
            case TOKEN_EXCHANGE_FAILED -> "Google 토큰 교환에 실패했습니다.";
            case MISSING_ACCESS_TOKEN -> "Google에서 액세스 토큰을 받지 못했습니다.";
            case MISSING_REFRESH_TOKEN -> "Google에서 갱신 토큰을 받지 못했습니다. 다시 동의해주세요.";
            case REQUIRED_SCOPE_MISSING -> "필수 YouTube 권한이 승인되지 않았습니다.";
            case CHANNEL_LOOKUP_FAILED -> "작성 채널 정보를 조회하지 못했습니다.";
            case CHANNEL_NOT_UNIQUE -> "작성 채널을 하나로 확정할 수 없습니다.";
            case INVALID_CHANNEL -> "Google이 반환한 채널 정보가 올바르지 않습니다.";
            case TOKEN_REVOKE_FAILED -> "Google 연결 해제에 실패했습니다. 잠시 후 다시 시도해주세요.";
            case OAUTH_NOT_CONNECTED -> "YouTube 작성 채널을 먼저 연결해주세요.";
        };
    }
}

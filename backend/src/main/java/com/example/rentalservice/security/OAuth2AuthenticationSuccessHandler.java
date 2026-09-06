package com.example.rentalservice.security;

import com.example.rentalservice.api.dto.TokenPairResponse;
import com.example.rentalservice.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;
    private final String frontendUrl;

    public OAuth2AuthenticationSuccessHandler(
            AuthService authService,
            @Value("${app.frontend-url}") String frontendUrl
    ) {
        this.authService = authService;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        OAuth2User principal = (OAuth2User) authentication.getPrincipal();
        String email = principal.getAttribute("email");
        String name = principal.getAttribute("name");
        if (email == null || email.isBlank()) {
            response.sendRedirect(frontendUrl + "/auth?oauth2Error=google_email_missing");
            return;
        }
        TokenPairResponse tokenPair = authService.loginWithOAuth2(email, name);
        String redirectUrl = frontendUrl
                + "/auth/callback"
                + "?accessToken=" + encode(tokenPair.accessToken())
                + "&refreshToken=" + encode(tokenPair.refreshToken())
                + "&email=" + encode(tokenPair.email())
                + "&role=" + encode(tokenPair.role().name());
        response.sendRedirect(redirectUrl);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}

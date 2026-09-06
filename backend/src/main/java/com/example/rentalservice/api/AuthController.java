package com.example.rentalservice.api;

import com.example.rentalservice.api.dto.LoginRequest;
import com.example.rentalservice.api.dto.LogoutRequest;
import com.example.rentalservice.api.dto.RefreshRequest;
import com.example.rentalservice.api.dto.RegisterRequest;
import com.example.rentalservice.api.dto.TokenPairResponse;
import com.example.rentalservice.api.dto.ChangePasswordRequest;
import com.example.rentalservice.api.dto.ConfirmTopUpRequest;
import com.example.rentalservice.api.dto.RequestTopUpCodeResponse;
import com.example.rentalservice.api.dto.TopUpBalanceRequest;
import com.example.rentalservice.api.dto.UpdateProfileRequest;
import com.example.rentalservice.service.AuthService;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public TokenPairResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public TokenPairResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public TokenPairResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request);
    }

    @PostMapping("/logout")
    public Map<String, String> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
        return Map.of("message", "Logout successful");
    }

    @GetMapping("/me")
    public Map<String, Object> me(Authentication authentication) {
        var user = authService.findByEmail(authentication.getName());
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("email", user.getEmail());
        response.put("fullName", user.getFullName());
        response.put("phoneNumber", user.getPhoneNumber());
        response.put("virtualBalance", user.getVirtualBalance());
        return response;
    }

    @PatchMapping("/me")
    public Map<String, Object> updateMe(@RequestBody UpdateProfileRequest request, Authentication authentication) {
        var current = authService.findByEmail(authentication.getName());
        var updated = authService.updateProfile(current, request);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("email", updated.getEmail());
        response.put("fullName", updated.getFullName());
        response.put("phoneNumber", updated.getPhoneNumber());
        response.put("virtualBalance", updated.getVirtualBalance());
        return response;
    }

    @PatchMapping("/me/password")
    public Map<String, String> changePassword(@Valid @RequestBody ChangePasswordRequest request, Authentication authentication) {
        var current = authService.findByEmail(authentication.getName());
        authService.changePassword(current, request);
        return Map.of("message", "Password changed successfully");
    }

    @PostMapping("/me/top-up/request-code")
    public RequestTopUpCodeResponse requestTopUpCode(@Valid @RequestBody TopUpBalanceRequest request, Authentication authentication) {
        var current = authService.findByEmail(authentication.getName());
        return authService.requestTopUpCode(current, request.amount());
    }

    @PostMapping("/me/top-up/confirm")
    public Map<String, Object> confirmTopUp(@Valid @RequestBody ConfirmTopUpRequest request, Authentication authentication) {
        var current = authService.findByEmail(authentication.getName());
        var updated = authService.confirmTopUp(current, request.amount(), request.code());
        return Map.of(
                "message", "Balance topped up successfully.",
                "virtualBalance", updated.getVirtualBalance()
        );
    }
}

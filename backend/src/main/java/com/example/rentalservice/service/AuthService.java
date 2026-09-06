package com.example.rentalservice.service;

import com.example.rentalservice.api.dto.LoginRequest;
import com.example.rentalservice.api.dto.LogoutRequest;
import com.example.rentalservice.api.dto.RequestTopUpCodeResponse;
import com.example.rentalservice.api.dto.RefreshRequest;
import com.example.rentalservice.api.dto.RegisterRequest;
import com.example.rentalservice.api.dto.TokenPairResponse;
import com.example.rentalservice.api.dto.ChangePasswordRequest;
import com.example.rentalservice.api.dto.UpdateProfileRequest;
import com.example.rentalservice.domain.RefreshToken;
import com.example.rentalservice.domain.RoleName;
import com.example.rentalservice.domain.User;
import com.example.rentalservice.domain.UserStatus;
import com.example.rentalservice.repository.RefreshTokenRepository;
import com.example.rentalservice.repository.UserRepository;
import com.example.rentalservice.security.JwtService;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
public class AuthService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,64}$");
    /** Format: "+375 (17|25|29|33|44) XXX-XX-XX" (literal spaces as shown). */
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+375 \\((17|25|29|33|44)\\) \\d{3}-\\d{2}-\\d{2}$");

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final MailService mailService;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            MailService mailService
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.mailService = mailService;
    }

    @Transactional
    public TokenPairResponse register(RegisterRequest request) {
        validateEmail(request.email());
        validatePasswordPolicy(request.password());
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ResponseStatusException(BAD_REQUEST, "Email already registered");
        }

        User user = new User();
        user.setEmail(request.email().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName().trim());
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedAt(Instant.now());
        user.setVirtualBalance(new BigDecimal("1500.00"));
        user.setRole(RoleName.USER);
        User saved = userRepository.save(user);
        return createTokenPair(saved);
    }

    @Transactional
    public TokenPairResponse login(LoginRequest request) {
        validateEmail(request.email());
        User user = userRepository.findByEmailIgnoreCase(request.email().trim().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Invalid credentials"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(UNAUTHORIZED, "Invalid credentials");
        }
        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new ResponseStatusException(FORBIDDEN, "User is blocked");
        }
        refreshTokenRepository.deleteByUser(user);
        return createTokenPair(user);
    }

    @Transactional
    public TokenPairResponse refresh(RefreshRequest request) {
        String refreshToken = request.refreshToken();
        if (!jwtService.isTokenValid(refreshToken) || !jwtService.isRefreshToken(refreshToken)) {
            throw new ResponseStatusException(UNAUTHORIZED, "Invalid refresh token");
        }

        RefreshToken savedToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Refresh token not found"));

        User user = savedToken.getUser();
        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new ResponseStatusException(FORBIDDEN, "User is blocked");
        }

        refreshTokenRepository.deleteByToken(refreshToken);
        return createTokenPair(user);
    }

    @Transactional
    public void logout(LogoutRequest request) {
        refreshTokenRepository.deleteByToken(request.refreshToken());
    }

    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "User not found"));
    }

    @Transactional
    public User updateProfile(User user, UpdateProfileRequest request) {
        String nextEmail = request.email() == null ? user.getEmail() : request.email().trim().toLowerCase();
        String nextFullName = request.fullName() == null ? user.getFullName() : request.fullName().trim();
        String nextPhone = request.phoneNumber() == null ? user.getPhoneNumber() : normalizePhone(request.phoneNumber());
        validateEmail(nextEmail);
        if (nextFullName.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Full name must not be blank");
        }
        if (!nextEmail.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmailIgnoreCase(nextEmail)) {
            throw new ResponseStatusException(BAD_REQUEST, "Email already registered");
        }
        user.setEmail(nextEmail);
        user.setFullName(nextFullName);
        user.setPhoneNumber(nextPhone);
        return userRepository.save(user);
    }

    @Transactional
    public void changePassword(User user, ChangePasswordRequest request) {
        if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(BAD_REQUEST, "Old password is incorrect");
        }
        validatePasswordPolicy(request.newPassword());
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(BAD_REQUEST, "New password must be different");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        refreshTokenRepository.deleteByUser(user);
    }

    @Transactional
    public RequestTopUpCodeResponse requestTopUpCode(User user, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ONE) < 0) {
            throw new ResponseStatusException(BAD_REQUEST, "Top-up amount must be at least 1");
        }
        if (amount.compareTo(new BigDecimal("100000")) > 0) {
            throw new ResponseStatusException(BAD_REQUEST, "Top-up amount is too large");
        }
        String code = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(5));
        user.setTopUpCode(code);
        user.setTopUpCodeExpiresAt(expiresAt);
        user.setPendingTopUpAmount(amount);
        userRepository.save(user);
        mailService.send(
                user.getEmail(),
                "Код подтверждения пополнения баланса",
                "Ваш код подтверждения: " + code + ". Сумма пополнения: " + amount + ". Срок действия до " + expiresAt + "."
        );
        return new RequestTopUpCodeResponse("Confirmation code sent to your email.", expiresAt);
    }

    @Transactional
    public User confirmTopUp(User user, BigDecimal amount, String code) {
        if (amount == null || amount.compareTo(BigDecimal.ONE) < 0) {
            throw new ResponseStatusException(BAD_REQUEST, "Top-up amount must be at least 1");
        }
        if (user.getTopUpCode() == null || user.getTopUpCodeExpiresAt() == null || user.getPendingTopUpAmount() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Request top-up confirmation code first");
        }
        if (user.getTopUpCodeExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(BAD_REQUEST, "Top-up confirmation code expired");
        }
        if (user.getPendingTopUpAmount().compareTo(amount) != 0) {
            throw new ResponseStatusException(BAD_REQUEST, "Top-up amount does not match requested amount");
        }
        String normalizedCode = normalizeCode(code);
        if (!user.getTopUpCode().equals(normalizedCode)) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid top-up confirmation code");
        }
        user.setVirtualBalance(user.getVirtualBalance().add(amount));
        user.setTopUpCode(null);
        user.setTopUpCodeExpiresAt(null);
        user.setPendingTopUpAmount(null);
        return userRepository.save(user);
    }

    @Transactional
    public TokenPairResponse loginWithOAuth2(String email, String fullName) {
        validateEmail(email);
        String normalizedEmail = email.trim().toLowerCase();
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseGet(() -> {
                    User created = new User();
                    created.setEmail(normalizedEmail);
                    created.setFullName((fullName == null || fullName.isBlank()) ? normalizedEmail : fullName.trim());
                    // OAuth2 users authenticate externally, local password is not used.
                    created.setPasswordHash(passwordEncoder.encode("oauth2-user-" + normalizedEmail));
                    created.setStatus(UserStatus.ACTIVE);
                    created.setCreatedAt(Instant.now());
                    created.setVirtualBalance(new BigDecimal("1500.00"));
                    created.setRole(RoleName.USER);
                    return userRepository.save(created);
                });
        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new ResponseStatusException(FORBIDDEN, "User is blocked");
        }
        refreshTokenRepository.deleteByUser(user);
        return createTokenPair(user);
    }

    public TokenPairResponse createTokenPair(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshTokenRaw = jwtService.generateRefreshToken(user);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(refreshTokenRaw);
        refreshToken.setExpiresAt(jwtService.extractExpiration(refreshTokenRaw));
        refreshTokenRepository.save(refreshToken);

        return new TokenPairResponse(
                accessToken,
                refreshTokenRaw,
                "Bearer",
                user.getEmail(),
                user.getRole()
        );
    }

    private void validateEmail(String email) {
        if (!EMAIL_PATTERN.matcher(email == null ? "" : email.trim()).matches()) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid email format");
        }
    }

    private void validatePasswordPolicy(String password) {
        if (!PASSWORD_PATTERN.matcher(password == null ? "" : password).matches()) {
            throw new ResponseStatusException(BAD_REQUEST, "Password must be 8-64 chars and include letters and digits");
        }
    }

    private String normalizePhone(String phone) {
        String value = phone == null ? "" : phone.trim();
        if (value.isBlank()) {
            return null;
        }
        if (!PHONE_PATTERN.matcher(value).matches()) {
            throw new ResponseStatusException(
                    BAD_REQUEST,
                    "Phone must match '+375 (17|25|29|33|44) XXX-XX-XX'"
            );
        }
        return value;
    }

    private String normalizeCode(String rawCode) {
        if (rawCode == null) {
            return "";
        }
        return rawCode.replaceAll("\\D", "");
    }
}

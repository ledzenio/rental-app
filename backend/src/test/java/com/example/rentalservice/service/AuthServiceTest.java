package com.example.rentalservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.rentalservice.api.dto.RequestTopUpCodeResponse;
import com.example.rentalservice.domain.RoleName;
import com.example.rentalservice.domain.User;
import com.example.rentalservice.domain.UserStatus;
import com.example.rentalservice.repository.RefreshTokenRepository;
import com.example.rentalservice.repository.UserRepository;
import com.example.rentalservice.security.JwtService;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private MailService mailService;

    @InjectMocks
    private AuthService authService;

    @Test
    void requestTopUpCodeSavesCodeAndSendsEmail() {
        User user = buildActiveUser();
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RequestTopUpCodeResponse response = authService.requestTopUpCode(user, new BigDecimal("120.50"));

        assertNotNull(response.expiresAt());
        assertEquals(new BigDecimal("120.50"), user.getPendingTopUpAmount());
        assertNotNull(user.getTopUpCode());
        assertNotNull(user.getTopUpCodeExpiresAt());
        verify(userRepository).save(user);
        verify(mailService).send(
                user.getEmail(),
                "Код подтверждения пополнения баланса",
                "Ваш код подтверждения: " + user.getTopUpCode()
                        + ". Сумма пополнения: 120.50. Срок действия до " + user.getTopUpCodeExpiresAt() + "."
        );
    }

    @Test
    void confirmTopUpAddsBalanceAndClearsPendingFields() {
        User user = buildActiveUser();
        user.setVirtualBalance(new BigDecimal("1000.00"));
        user.setPendingTopUpAmount(new BigDecimal("200.00"));
        user.setTopUpCode("123456");
        user.setTopUpCodeExpiresAt(Instant.now().plusSeconds(120));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User updated = authService.confirmTopUp(user, new BigDecimal("200.00"), "123456");

        assertEquals(new BigDecimal("1200.00"), updated.getVirtualBalance());
        assertNull(updated.getTopUpCode());
        assertNull(updated.getTopUpCodeExpiresAt());
        assertNull(updated.getPendingTopUpAmount());
        verify(userRepository).save(user);
    }

    @Test
    void confirmTopUpRejectsWhenCodeExpired() {
        User user = buildActiveUser();
        user.setPendingTopUpAmount(new BigDecimal("50.00"));
        user.setTopUpCode("555555");
        user.setTopUpCodeExpiresAt(Instant.now().minusSeconds(1));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> authService.confirmTopUp(user, new BigDecimal("50.00"), "555555")
        );

        assertEquals(400, ex.getStatusCode().value());
    }

    private static User buildActiveUser() {
        User user = new User();
        user.setEmail("user@test.local");
        user.setPasswordHash("hash");
        user.setFullName("Test User");
        user.setStatus(UserStatus.ACTIVE);
        user.setRole(RoleName.USER);
        user.setCreatedAt(Instant.now());
        user.setVirtualBalance(new BigDecimal("1500.00"));
        return user;
    }
}

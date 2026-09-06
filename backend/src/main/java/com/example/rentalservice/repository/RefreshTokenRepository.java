package com.example.rentalservice.repository;

import com.example.rentalservice.domain.RefreshToken;
import com.example.rentalservice.domain.User;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    void deleteByUser(User user);
    void deleteByToken(String token);
    void deleteByExpiresAtBefore(Instant now);
}

package com.example.rentalservice.service;

import com.example.rentalservice.api.dto.AdminCreateUserRequest;
import com.example.rentalservice.api.dto.AdminUpdateUserRequest;
import com.example.rentalservice.api.dto.AdminUserResponse;
import com.example.rentalservice.domain.User;
import com.example.rentalservice.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ManagerUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public ManagerUserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public Page<AdminUserResponse> listUsers(String query, Pageable pageable) {
        Specification<User> spec = Specification.where(null);
        if (query != null && !query.isBlank()) {
            String like = "%" + query.trim().toLowerCase() + "%";
            spec = spec.and((root, q, cb) -> cb.or(
                    cb.like(cb.lower(root.get("email")), like),
                    cb.like(cb.lower(root.get("fullName")), like)
            ));
        }
        return userRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public AdminUserResponse getUser(Long userId) {
        return toResponse(loadUser(userId));
    }

    @Transactional
    public AdminUserResponse updateUser(Long userId, AdminUpdateUserRequest request) {
        User user = loadUser(userId);
        String nextEmail = request.email().trim().toLowerCase();
        if (!nextEmail.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmailIgnoreCase(nextEmail)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already registered");
        }
        user.setEmail(nextEmail);
        user.setFullName(request.fullName().trim());
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        user.setStatus(request.status());
        user.setRole(request.role());
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public AdminUserResponse createUser(AdminCreateUserRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already registered");
        }
        User user = new User();
        user.setEmail(request.email().trim().toLowerCase());
        user.setFullName(request.fullName().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setStatus(request.status());
        user.setRole(request.role());
        user.setCreatedAt(Instant.now());
        user.setVirtualBalance(new BigDecimal("1500.00"));
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = loadUser(userId);
        userRepository.delete(user);
    }

    @Transactional(readOnly = true)
    public User loadCurrentManager(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Manager not found"));
    }

    private User loadUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private AdminUserResponse toResponse(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getStatus(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}

package com.example.rentalservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private UserStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "virtual_balance", nullable = false, precision = 12, scale = 2)
    private BigDecimal virtualBalance = BigDecimal.ZERO;

    @Column(name = "top_up_code", length = 16)
    private String topUpCode;

    @Column(name = "top_up_code_expires_at")
    private Instant topUpCodeExpiresAt;

    @Column(name = "pending_top_up_amount", precision = 12, scale = 2)
    private BigDecimal pendingTopUpAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 64)
    private RoleName role;

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public RoleName getRole() {
        return role;
    }

    public void setRole(RoleName role) {
        this.role = role;
    }

    public BigDecimal getVirtualBalance() {
        return virtualBalance;
    }

    public void setVirtualBalance(BigDecimal virtualBalance) {
        this.virtualBalance = virtualBalance;
    }

    public String getTopUpCode() {
        return topUpCode;
    }

    public void setTopUpCode(String topUpCode) {
        this.topUpCode = topUpCode;
    }

    public Instant getTopUpCodeExpiresAt() {
        return topUpCodeExpiresAt;
    }

    public void setTopUpCodeExpiresAt(Instant topUpCodeExpiresAt) {
        this.topUpCodeExpiresAt = topUpCodeExpiresAt;
    }

    public BigDecimal getPendingTopUpAmount() {
        return pendingTopUpAmount;
    }

    public void setPendingTopUpAmount(BigDecimal pendingTopUpAmount) {
        this.pendingTopUpAmount = pendingTopUpAmount;
    }
}

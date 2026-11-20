package com.bhoomi.user;

import com.bhoomi.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class UserEntity extends BaseEntity {

    @Column(nullable = false, length = 320)
    private String email;

    @Column(length = 32)
    private String phone;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 160)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private UserRole role = UserRole.BUYER;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "credential_version", nullable = false)
    private int credentialVersion;

    protected UserEntity() {
    }

    public UserEntity(String email, String phone, String passwordHash, String fullName, UserRole role) {
        this.email = email;
        this.phone = phone;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = role;
    }

    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getPasswordHash() { return passwordHash; }
    public String getFullName() { return fullName; }
    public UserRole getRole() { return role; }
    public boolean isEnabled() { return enabled; }
    public int getCredentialVersion() { return credentialVersion; }

    public void changePassword(String passwordHash) {
        this.passwordHash = passwordHash;
        this.credentialVersion++;
    }

    public void updateProfile(String fullName, String phone) {
        this.fullName = fullName;
        this.phone = phone;
    }
}

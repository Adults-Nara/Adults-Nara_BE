package com.ott.common.persistence.entity;

import com.ott.common.persistence.enums.UserRole;
import com.ott.common.persistence.enums.BanStatus;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    private Long id;

    @Column(unique = true, nullable = false, length = 255)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(name = "profile_image_url", columnDefinition = "TEXT")
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "user_role")
    private UserRole role;

    @Column(name = "oauth_provider", length = 20)
    private String oauthProvider;

    @Column(name = "oauth_id", length = 255)
    private String oauthId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "ban_status")
    private BanStatus banned;

    @Column(name = "banned_until")
    private OffsetDateTime bannedUntil;

    @Column(name = "ban_reason", columnDefinition = "TEXT")
    private String banReason;

    @Column(name = "banned_at")
    private OffsetDateTime bannedAt;

    @Column(name = "banned_by")
    private Long bannedBy;

    @Column(name = "created_at", updatable = false, nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected User() {}

    public User(Long id, String email, String passwordHash, String nickname, UserRole role) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.nickname = nickname;
        this.role = role;
        this.oauthProvider = null;
        this.oauthId = null;
        this.banned = BanStatus.ACTIVE;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    public User(Long id, String email, String nickname, String oauthProvider, String oauthId) {
        this.id = id;
        this.email = email;
        this.passwordHash = null;
        this.nickname = nickname;
        this.role = UserRole.VIEWER;
        this.oauthProvider = oauthProvider;
        this.oauthId = oauthId;
        this.banned = BanStatus.ACTIVE;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    // ✅ JPA 생명주기 콜백 - 엔티티 업데이트 시 자동 호출
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    // ===== 비즈니스 메서드 (updatedAt 수동 설정 제거) =====

    public void suspend7Days(String reason, Long adminId) {
        this.banned = BanStatus.SUSPENDED_7;
        this.bannedUntil = OffsetDateTime.now().plusDays(7);
        this.banReason = reason;
        this.bannedAt = OffsetDateTime.now();
        this.bannedBy = adminId;
        // ✅ updatedAt 수동 설정 제거 (@PreUpdate가 자동 처리)
    }

    public void suspend15Days(String reason, Long adminId) {
        this.banned = BanStatus.SUSPENDED_15;
        this.bannedUntil = OffsetDateTime.now().plusDays(15);
        this.banReason = reason;
        this.bannedAt = OffsetDateTime.now();
        this.bannedBy = adminId;
    }

    public void suspend30Days(String reason, Long adminId) {
        this.banned = BanStatus.SUSPENDED_30;
        this.bannedUntil = OffsetDateTime.now().plusDays(30);
        this.banReason = reason;
        this.bannedAt = OffsetDateTime.now();
        this.bannedBy = adminId;
    }

    public void banPermanently(String reason, Long adminId) {
        this.banned = BanStatus.PERMANENTLY_BANNED;
        this.bannedUntil = null;
        this.banReason = reason;
        this.bannedAt = OffsetDateTime.now();
        this.bannedBy = adminId;
    }

    public void activate() {
        this.banned = BanStatus.ACTIVE;
        this.bannedUntil = null;
        this.banReason = null;
        this.bannedAt = null;
        this.bannedBy = null;
    }

    public void deactivate() {
        this.banned = BanStatus.DEACTIVATED;
        this.bannedUntil = null;
        this.banReason = "사용자 요청";
        this.bannedAt = OffsetDateTime.now();
    }

    public void delete(String reason) {
        this.banned = BanStatus.DELETED;
        this.bannedUntil = null;
        this.banReason = reason;
        this.bannedAt = OffsetDateTime.now();
        this.email = "deleted_" + this.id + "@deleted.com";
    }

    public void updateNickname(String newNickname) {
        this.nickname = newNickname;
    }

    public void updatePassword(String newPasswordHash) {
        if (isOAuthUser()) {
            throw new IllegalStateException("OAuth 사용자는 비밀번호를 변경할 수 없습니다.");
        }
        this.passwordHash = newPasswordHash;
    }

    public void updateProfileImage(String newProfileImageUrl) {
        this.profileImageUrl = newProfileImageUrl;
    }

    public boolean isOAuthUser() {
        return this.oauthProvider != null;
    }

    public boolean isDeleted() {
        return this.banned == BanStatus.DELETED;
    }

    public boolean isActive() {
        return this.banned == BanStatus.ACTIVE;
    }

    public boolean isSuspended() {
        return this.banned.isSuspended();
    }

    // Getter
    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getNickname() { return nickname; }
    public String getProfileImageUrl() { return profileImageUrl; }
    public UserRole getRole() { return role; }
    public String getOauthProvider() { return oauthProvider; }
    public String getOauthId() { return oauthId; }
    public BanStatus getBanned() { return banned; }
    public OffsetDateTime getBannedUntil() { return bannedUntil; }
    public String getBanReason() { return banReason; }
    public OffsetDateTime getBannedAt() { return bannedAt; }
    public Long getBannedBy() { return bannedBy; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
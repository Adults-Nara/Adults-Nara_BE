package com.ott.common.persistence.entity;

import com.ott.common.persistence.enums.BanStatus;
import com.ott.common.persistence.enums.UserRole;
import com.ott.common.util.IdGenerator;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_email", columnNames = "email")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class User {

    @Id
    @Column(name = "user_id", nullable = false)
    private Long id;

    @Column(name = "email", length = 225, nullable = false)
    private String email;

    @Column(name = "password_hash", length = 225)
    private String passwordHash;

    @Column(name = "nickname", length = 50, nullable = false)
    private String nickname;

    @Column(name = "profile_image_url", columnDefinition = "TEXT")
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRole role;

    @Column(name = "oauth_provider", length = 20)
    private String oauthProvider;

    @Column(name = "oauth_id", length = 225)
    private String oauthId;

    @Column(name = "deleted", nullable = false)
    private boolean deleted;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "banned", nullable = false)
    private BanStatus banned;

    @Column(name = "banned_until")
    private OffsetDateTime bannedUntil;

    @Column(name = "ban_reason", columnDefinition = "TEXT")
    private String banReason;

    @Column(name = "banned_at")
    private OffsetDateTime bannedAt;

    public User(String email, String nickname, String passwordHash, UserRole userRole) {
        this.email = email;
        this.nickname = nickname;
        this.role = userRole;
        this.banned = BanStatus.ACTIVE;
        this.deleted = false;
        this.passwordHash = passwordHash;
    }

    @PrePersist
    private void prePersist() {
        if (id == null) id = IdGenerator.generate();
        if (role == null) role = UserRole.VIEWER;
        if (banned == null) banned = BanStatus.ACTIVE;
    }

    // ===== 도메인 메서드 =====
    public void changeProfileImage(String url) { this.profileImageUrl = url; }
    public void changeNickname(String nickname) { this.nickname = nickname; }
    public void setOAuth(String provider, String oauthId) { this.oauthProvider = provider; this.oauthId = oauthId; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setBanStatus(BanStatus status) { this.banned = status; }

    public void markDeleted(String reason) {
        this.deleted = true;
        this.banned = BanStatus.DELETED;
        this.banReason = reason;
        this.bannedAt = OffsetDateTime.now();
    }

    public void suspend(BanStatus status, OffsetDateTime until, String reason) {
        this.banned = status;
        this.bannedUntil = until;
        this.banReason = reason;
        this.bannedAt = OffsetDateTime.now();
    }

    public void activate() {
        this.banned = BanStatus.ACTIVE;
        this.bannedUntil = null;
        this.banReason = null;
        this.bannedAt = null;
    }

    public void deactivate() {
        this.banned = BanStatus.DELETED;
        this.bannedUntil = null;
        this.banReason = null;
        this.bannedAt = OffsetDateTime.now();
    }
}

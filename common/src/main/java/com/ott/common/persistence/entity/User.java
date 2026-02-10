package com.ott.common.persistence.entity;

import com.ott.common.persistence.enums.AccountStatus;
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
    private AccountStatus banned;

    @Column(name = "banned_until")
    private OffsetDateTime bannedUntil;

    @Column(name = "ban_reason", columnDefinition = "TEXT")
    private String banReason;

    @Column(name = "banned_at")
    private OffsetDateTime bannedAt;

    public User(String email, String nickname) {
        this.email = email;
        this.nickname = nickname;
        this.role = UserRole.VIEWER;
        this.banned = AccountStatus.ACTIVE;
        this.deleted = false;
    }

    @PrePersist
    private void prePersist() {
        if (id == null) id = IdGenerator.generate();
        if (role == null) role = UserRole.VIEWER;
        if (banned == null) banned = AccountStatus.ACTIVE;
        // createdAt/updatedAt은 DB DEFAULT NOW()에 맡김
    }

    // ===== 도메인 메서드 =====
    public void changeProfileImage(String url) { this.profileImageUrl = url; }
    public void changeNickname(String nickname) { this.nickname = nickname; }
    public void setOAuth(String provider, String oauthId) { this.oauthProvider = provider; this.oauthId = oauthId; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public void markDeleted(String reason) {
        this.deleted = true;
        this.banned = AccountStatus.DELETED;
        this.banReason = reason;
        this.bannedAt = OffsetDateTime.now();
    }

    public void suspend(AccountStatus status, OffsetDateTime until, String reason) {
        this.banned = status;
        this.bannedUntil = until;
        this.banReason = reason;
        this.bannedAt = OffsetDateTime.now();
    }

    public void activate() {
        this.banned = AccountStatus.ACTIVE;
        this.bannedUntil = null;
        this.banReason = null;
        this.bannedAt = null;
    }
}

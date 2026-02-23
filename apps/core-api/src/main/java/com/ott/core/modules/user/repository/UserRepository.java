package com.ott.core.modules.user.repository;

import com.ott.common.persistence.entity.User;
import com.ott.common.persistence.enums.BanStatus;
import com.ott.common.persistence.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // OAuth 사용자 조회 (카카오 로그인 시 기존 사용자 찾기)
    @Query("SELECT u FROM User u WHERE u.oauthProvider = :oauthProvider AND u.oauthId = :oauthId AND u.deleted = false")
    Optional<User> findByOauthProviderAndOauthId(
            @Param("oauthProvider") String oauthProvider,
            @Param("oauthId") String oauthId
    );
    // ✅ deleted = false 조건으로 변경
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.deleted = false")
    Optional<User> findByEmailAndNotDeleted(@Param("email") String email);

    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.email = :email AND u.deleted = false")
    boolean existsByEmailAndNotDeleted(@Param("email") String email);

    @Query("SELECT u FROM User u WHERE u.banned = :banned")
    Page<User> findByBanned(@Param("banned") BanStatus banned, Pageable pageable);

    long countByBanned(BanStatus banned);

    long countByRole(UserRole role);

    Optional<User> findByEmail(String email);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE User u SET u.banned = :banStatus, u.banReason = :banReason, " +
            "u.bannedUntil = :bannedUtil, u.bannedAt = :bannedAt " +
            "WHERE u.id IN :userIds AND u.deleted = false AND u.role != 'ADMIN'")
    void updateBanStatus(@Param("banStatus") BanStatus banStatus,
                         @Param("banReason") String banReason,
                         @Param("bannedUtil") OffsetDateTime bannedUtil,
                         @Param("bannedAt") OffsetDateTime bannedAt,
                         @Param("userIds") List<Long> userIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE User u SET u.deleted = true, u.banned = :banStatus, u.bannedAt = :bannedAt " +
            "WHERE u.id IN :userIds AND u.deleted = false AND u.role != 'ADMIN'")
    void softDeleteUserByAdmin(@Param("userIds") List<Long> userIds,
                               @Param("banStatus") BanStatus banStatus,
                               @Param("bannedAt") OffsetDateTime bannedAt);
}
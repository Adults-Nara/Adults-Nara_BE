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

    // ====== 활성 유저 조회 (deleted=false) ======

    @Query("SELECT u FROM User u WHERE u.oauthProvider = :oauthProvider AND u.oauthId = :oauthId AND u.deleted = false")
    Optional<User> findByOauthProviderAndOauthId(
            @Param("oauthProvider") String oauthProvider,
            @Param("oauthId") String oauthId
    );

    @Query("SELECT u FROM User u WHERE u.email = :email AND u.deleted = false")
    Optional<User> findByEmailAndNotDeleted(@Param("email") String email);

    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.email = :email AND u.deleted = false")
    boolean existsByEmailAndNotDeleted(@Param("email") String email);

    /**
     * 이메일 존재 여부 확인 - 탈퇴(deleted=true) 유저 포함
     * 백오피스 회원가입/이메일 중복체크에서 사용
     * 탈퇴한 이메일은 본인 포함 누구도 재사용 불가
     */
    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    // ====== 탈퇴 유저 포함 조회 (재가입 처리용) ======

    /**
     * oauth_id로 유저 조회 - 탈퇴(deleted=true) 유저 포함
     * 재가입 시 기존 레코드를 재활성화하기 위해 사용
     */
    @Query("SELECT u FROM User u WHERE u.oauthProvider = :oauthProvider AND u.oauthId = :oauthId")
    Optional<User> findByOauthProviderAndOauthIdIncludingDeleted(
            @Param("oauthProvider") String oauthProvider,
            @Param("oauthId") String oauthId
    );

    /**
     * 이메일로 유저 조회 - 탈퇴(deleted=true) 유저 포함
     * 재가입 시 이메일 기반 계정 연동을 위해 사용
     */
    @Query("SELECT u FROM User u WHERE u.email = :email")
    Optional<User> findByEmailIncludingDeleted(@Param("email") String email);

    // ====== 관리 쿼리 ======

    @Query("SELECT u FROM User u WHERE u.banned = :banned")
    Page<User> findByBanned(@Param("banned") BanStatus banned, Pageable pageable);

    long countByBanned(BanStatus banned);

    long countByRole(UserRole role);

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
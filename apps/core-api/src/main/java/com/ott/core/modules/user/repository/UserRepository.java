package com.ott.core.modules.user.repository;

import com.ott.common.persistence.entity.User;
import com.ott.common.persistence.enums.BanStatus;
import com.ott.common.persistence.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT u FROM User u WHERE u.email = :email AND u.banned != 'DELETED'")
    Optional<User> findByEmailAndNotDeleted(@Param("email") String email);

    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.email = :email AND u.banned != 'DELETED'")
    boolean existsByEmailAndNotDeleted(@Param("email") String email);

    @Query("SELECT u FROM User u WHERE u.banned != 'DELETED'")
    Page<User> findAllNotDeleted(Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.role = :role AND u.banned != 'DELETED'")
    Page<User> findByRoleAndNotDeleted(@Param("role") UserRole role, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.banned = :banned")
    Page<User> findByBanned(@Param("banned") BanStatus banned, Pageable pageable);

    long countByBanned(BanStatus banned);

    long countByRole(UserRole role);
}
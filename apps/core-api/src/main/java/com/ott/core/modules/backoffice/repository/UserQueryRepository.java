package com.ott.core.modules.backoffice.repository;

import com.ott.common.persistence.enums.UserRole;
import com.ott.core.modules.backoffice.dto.AdminUserResponse;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.ott.common.persistence.entity.QUser.*;

@Repository
@RequiredArgsConstructor
public class UserQueryRepository {

    private final JPAQueryFactory query;

    public Page<AdminUserResponse> findAllUsers(UserRole userRole, String keyword, Pageable pageable) {
        BooleanBuilder where = new BooleanBuilder();
        where.and(user.deleted.eq(false));
        where.and(user.role.eq(userRole));

        if (keyword != null && !keyword.isBlank()) {
            String escaped = keyword.replace("%", "\\%").replace("_", "\\_");
            String pattern = "%" + escaped + "%";
            where.and(
                    user.nickname.like(pattern, '\\')
                            .or(user.email.like(pattern, '\\'))
            );
        }

        Long total = query
                .select(user.id.count())
                .from(user)
                .where(where)
                .fetchOne();

        if (total == null || total == 0) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        List<AdminUserResponse> content = query
                .select(Projections.constructor(AdminUserResponse.class,
                        user.id.stringValue(),
                        user.profileImageUrl,
                        user.nickname,
                        user.email,
                        user.banned,
                        user.createdAt
                ))
                .from(user)
                .where(where)
                .orderBy(toOrderSpecifier(pageable))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        return new PageImpl<>(content, pageable, total);
    }

    private OrderSpecifier<?> toOrderSpecifier(Pageable pageable) {
        if (pageable.isUnpaged()) {
            return user.createdAt.desc();
        }
        Sort.Order order = pageable.getSort().iterator().next();
        boolean asc = order.isAscending();

        return switch (order.getProperty()) {
            case "createdAt" -> asc ? user.createdAt.asc() : user.createdAt.desc();
            case "nickname" -> asc ? user.nickname.asc() : user.nickname.desc();
            case "email" -> asc ? user.email.asc() : user.email.desc();
            default -> user.createdAt.desc();
        };
    }
}

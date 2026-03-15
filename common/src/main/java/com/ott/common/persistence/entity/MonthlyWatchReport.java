package com.ott.common.persistence.entity;

import com.ott.common.persistence.base.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 월간 태그별 시청 통계
 */
@Entity
@Table(
        name = "monthly_watch_report",
        uniqueConstraints = @UniqueConstraint(
                name = "monthly_watch_report_unique",
                columnNames = {"user_id", "stats_year", "stats_month", "tag_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MonthlyWatchReport extends BaseEntity {

    @Id
    @Column(name = "monthly_watch_report_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "stats_year", nullable = false)
    private Integer statsYear;

    @Column(name = "stats_month", nullable = false)
    private Integer statsMonth;

    @Column(name = "tag_id", nullable = false)
    private Long tagId;

    @Column(name = "tag_name", nullable = false, length = 100)
    private String tagName;

    @Column(name = "total_watch_seconds", nullable = false)
    private Long totalWatchSeconds;

    @Column(name = "watch_count", nullable = false)
    private Integer watchCount;

    @Builder
    public MonthlyWatchReport(
            Long id, Long userId, Integer statsYear, Integer statsMonth,
            Long tagId, String tagName, Long totalWatchSeconds, Integer watchCount) {
        this.id = id;
        this.userId = userId;
        this.statsYear = statsYear;
        this.statsMonth = statsMonth;
        this.tagId = tagId;
        this.tagName = tagName;
        this.totalWatchSeconds = totalWatchSeconds;
        this.watchCount = watchCount;
    }

}

package com.ott.common.persistence.entity;

import com.ott.common.persistence.base.BaseEntity;
import com.ott.common.util.IdGenerator;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "tag_stats",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_tag_stats_user_tag_date",
                        columnNames = {"user_id", "tag_id", "stats_date"}
                )
        }
)
public class TagStats extends BaseEntity {

    @Id
    @Column(name = "tag_stats_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "stats_date", nullable = false)
    private LocalDate statsDate;

    @Column(name = "total_view_time", nullable = false)
    private Integer totalViewTime;

    @Column(name = "view_count", nullable = false)
    private Integer viewCount;

    /** 완주 횟수 (WatchHistory.completed = true 기준) */
    @Column(name = "completed_count", nullable = false)
    private Integer completedCount;

    public TagStats(Tag tag, User user, LocalDate statsDate) {
        this.tag            = tag;
        this.user           = user;
        this.statsDate      = statsDate;
        this.totalViewTime  = 0;
        this.viewCount      = 0;
        this.completedCount = 0;
    }

    @PrePersist
    private void prePersist() {
        if (id == null)             id             = IdGenerator.generate();
        if (totalViewTime == null)  totalViewTime  = 0;
        if (viewCount == null)      viewCount      = 0;
        if (completedCount == null) completedCount = 0;
    }

}
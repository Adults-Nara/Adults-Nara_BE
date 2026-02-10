package com.ott.common.persistence.entity;

import com.ott.common.util.IdGenerator;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Getter
@NoArgsConstructor (access = AccessLevel.PROTECTED)
@Table(name = "tag_stats")
public class TagStats {

    @Id
    @Column(name = "tag_stats_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "stats_date")
    private OffsetDateTime statsDate;

    @Column(name = "total_view_time")
    private Integer totalViewTime;

    @Column(name = "view_count")
    private Integer viewCount;

    public TagStats(Tag tag, User user, OffsetDateTime statsDate) {
        this.tag = tag;
        this.user = user;
        this.statsDate = statsDate;
        this.totalViewTime = 0;
        this.viewCount = 0;
    }

    @PrePersist
    private void prePersist() {
        if (id == null) id = IdGenerator.generate();
        if (totalViewTime == null) totalViewTime = 0;
        if (viewCount == null) viewCount = 0;
    }

    public void addView(int seconds) {
        this.totalViewTime = (this.totalViewTime == null ? 0 : this.totalViewTime) + seconds;
        this.viewCount = (this.viewCount == null ? 0 : this.viewCount) + 1;
    }
}

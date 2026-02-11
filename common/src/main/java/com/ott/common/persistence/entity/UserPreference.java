package com.ott.common.persistence.entity;

import com.ott.common.persistence.base.BaseEntity;
import com.ott.common.util.IdGenerator;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_preference")
public class UserPreference extends BaseEntity {

    @Id
    @Column(name = "user_preference_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    private Double score;


    public UserPreference(User user, Tag tag, Double score) {
        this.user = user;
        this.tag = tag;
        this.score = score;
    }

    @PrePersist
    private void prePersist() {
        if (id == null) id = IdGenerator.generate();
    }

    public void updateScore(Double score) {
        this.score = score;
    }
}




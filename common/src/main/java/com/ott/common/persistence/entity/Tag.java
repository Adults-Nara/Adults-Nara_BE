package com.ott.common.persistence.entity;

import com.ott.common.util.IdGenerator;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tag")
public class Tag {

    @Id
    @Column(name = "tag_id")
    private Long id;

    @Column(name = "tag_name", nullable = false)
    private String tagName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Tag parent;

    @Column(name = "depth")
    private Integer depth;

    public Tag(String tagName) {
        this.tagName = tagName;
    }

    @PrePersist
    private void prePersist() {
        if (id == null) id = IdGenerator.generate();
    }

    public void setParent(Tag parent) { this.parent = parent; }
    public void setDepth(Integer depth) { this.depth = depth; }
}
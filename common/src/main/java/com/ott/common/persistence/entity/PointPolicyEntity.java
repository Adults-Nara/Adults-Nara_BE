package com.ott.common.persistence.entity;

import com.ott.common.persistence.enums.PointPolicy;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "point_policies")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointPolicyEntity {
    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "policy_name", nullable = false, length = 50)
    private PointPolicy policyName;
    @Column(name = "policy_value", nullable = false)
    private int policyValue;
    @Column(name = "description", length = 100)
    private String description;
    @Builder
    public PointPolicyEntity(PointPolicy policyName, int policyValue, String description) {
        this.policyName = policyName;
        this.policyValue = policyValue;
        this.description = description;
    }
    public void updateValue(int newValue) {
        this.policyValue = newValue;
    }
}
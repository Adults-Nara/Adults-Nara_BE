package com.ott.core.modules.point.dto;

import com.ott.common.persistence.enums.PointPolicy;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PointPolicyUpdateRequest {
    private PointPolicy policyName;
    private int policyValue;
}

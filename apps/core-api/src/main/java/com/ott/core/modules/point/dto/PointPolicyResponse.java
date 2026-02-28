package com.ott.core.modules.point.dto;

import com.ott.common.persistence.enums.PointPolicy;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PointPolicyResponse {
    private PointPolicy policyName;
    private String description;
    private int policyValue;
}

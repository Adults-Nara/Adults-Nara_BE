package com.ott.core.modules.point.dto;

import com.ott.common.persistence.enums.PointPolicy;
import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;

@Getter
@Builder
public class PointPolicyResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private PointPolicy policyName;
    private String description;
    private int policyValue;
}

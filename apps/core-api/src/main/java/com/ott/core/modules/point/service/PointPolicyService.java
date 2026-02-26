package com.ott.core.modules.point.service;

import com.ott.common.persistence.entity.PointPolicyEntity;
import com.ott.common.persistence.enums.PointPolicy;
import com.ott.core.modules.point.repository.PointPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PointPolicyService {
    private final PointPolicyRepository policyRepository;
    @Transactional(readOnly = true)
    public int getPolicyValue(PointPolicy policy) {
        return policyRepository.findById(policy)
                .map(PointPolicyEntity::getPolicyValue)
                .orElse(policy.getValue());
    }
    @Transactional
    public void updatePolicyValue(PointPolicy policy, int newValue) {
        PointPolicyEntity entity = policyRepository.findById(policy)
                .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 정책입니다: " + policy));

        entity.updateValue(newValue);
    }
}

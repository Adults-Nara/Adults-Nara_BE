package com.ott.core.modules.point.service;

import com.ott.common.error.BusinessException;
import com.ott.common.error.ErrorCode;
import com.ott.common.persistence.entity.PointPolicyEntity;
import com.ott.common.persistence.enums.PointPolicy;
import com.ott.core.modules.point.dto.PointPolicyResponse;
import com.ott.core.modules.point.repository.PointPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PointPolicyService {
    private final PointPolicyRepository policyRepository;

    @Cacheable(value = "pointPolicy", key = "#policy.name()")
    @Transactional(readOnly = true)
    public int getPolicyValue(PointPolicy policy) {
        return policyRepository.findById(policy)
                .map(PointPolicyEntity::getPolicyValue)
                .orElse(policy.getValue());
    }

    @Cacheable(value = "allPointPolicies")
    @Transactional(readOnly = true)
    public List<PointPolicyResponse> getAllPolicies() {
        return policyRepository.findAll().stream()
                .map(entity -> PointPolicyResponse.builder()
                        .policyName(entity.getPolicyName())
                        .description(entity.getDescription())
                        .policyValue(entity.getPolicyValue())
                        .build())
                .toList();
    }

    @CacheEvict(value = {"pointPolicy", "allPointPolicies"}, key = "#policy.name()", allEntries = true)
    @Transactional
    public void updatePolicyValue(PointPolicy policy, int newValue) {
        PointPolicyEntity entity = policyRepository.findById(policy)
                .orElseThrow(() -> new BusinessException(ErrorCode.POLICY_NOT_FOUND));
        entity.updateValue(newValue);
    }
}

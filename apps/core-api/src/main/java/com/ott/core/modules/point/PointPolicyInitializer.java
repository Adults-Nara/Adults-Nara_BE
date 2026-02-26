package com.ott.core.modules.point;

import com.ott.common.persistence.entity.PointPolicyEntity;
import com.ott.common.persistence.enums.PointPolicy;
import com.ott.core.modules.point.repository.PointPolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PointPolicyInitializer implements ApplicationRunner {
    private final PointPolicyRepository policyRepository;
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (PointPolicy policy : PointPolicy.values()) {
            if (!policyRepository.existsById(policy)) {
                // Enum에 하드코딩된 값을 최초 1회만 DB 초기값으로 사용함
                PointPolicyEntity entity = PointPolicyEntity.builder()
                        .policyName(policy)
                        .policyValue(policy.getValue())
                        .description(policy.getDescription())
                        .build();
                policyRepository.save(entity);
            }
        }
    }
}
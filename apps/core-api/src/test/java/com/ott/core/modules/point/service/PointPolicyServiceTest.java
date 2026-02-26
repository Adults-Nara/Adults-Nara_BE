package com.ott.core.modules.point.service;

import com.ott.common.persistence.entity.PointPolicyEntity;
import com.ott.common.persistence.enums.PointPolicy;
import com.ott.core.modules.point.dto.PointPolicyResponse;
import com.ott.core.modules.point.repository.PointPolicyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PointPolicyServiceTest {

    @InjectMocks
    private PointPolicyService pointPolicyService;

    @Mock
    private PointPolicyRepository policyRepository;

    @Test
    @DisplayName("DB에 저장된 특정 정책 값을 정상 조회한다")
    void getPolicyValue_Success_FromDB() {
        // given
        PointPolicy policy = PointPolicy.AD_REWARD;
        int expectedValue = 15;
        PointPolicyEntity entity = PointPolicyEntity.builder()
                .policyName(policy)
                .policyValue(expectedValue)
                .build();
        given(policyRepository.findById(policy)).willReturn(Optional.of(entity));

        // when
        int actualValue = pointPolicyService.getPolicyValue(policy);

        // then
        assertThat(actualValue).isEqualTo(expectedValue);
    }

    @Test
    @DisplayName("DB에 저장된 정책이 없을 경우 Enum 기본값을 반환한다")
    void getPolicyValue_FallbackToEnum() {
        // given
        PointPolicy policy = PointPolicy.DAILY_AD_LIMIT;
        given(policyRepository.findById(policy)).willReturn(Optional.empty());

        // when
        int actualValue = pointPolicyService.getPolicyValue(policy);

        // then
        assertThat(actualValue).isEqualTo(policy.getValue()); // Enum의 기본값
    }

    @Test
    @DisplayName("모든 정책 목록을 정상 조회한다")
    void getAllPolicies_Success() {
        // given
        PointPolicyEntity entity1 = PointPolicyEntity.builder()
                .policyName(PointPolicy.AD_REWARD)
                .policyValue(15)
                .description("광고 보상")
                .build();
        PointPolicyEntity entity2 = PointPolicyEntity.builder()
                .policyName(PointPolicy.DAILY_AD_LIMIT)
                .policyValue(10)
                .description("일일 제한")
                .build();

        given(policyRepository.findAll()).willReturn(List.of(entity1, entity2));

        // when
        List<PointPolicyResponse> responses = pointPolicyService.getAllPolicies();

        // then
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getPolicyName()).isEqualTo(PointPolicy.AD_REWARD);
        assertThat(responses.get(0).getPolicyValue()).isEqualTo(15);
        assertThat(responses.get(1).getPolicyName()).isEqualTo(PointPolicy.DAILY_AD_LIMIT);
        assertThat(responses.get(1).getPolicyValue()).isEqualTo(10);
    }

    @Test
    @DisplayName("특정 정책 값을 정상 수정한다")
    void updatePolicyValue_Success() {
        // given
        PointPolicy policy = PointPolicy.AD_REWARD;
        int newValue = 20;

        PointPolicyEntity entity = PointPolicyEntity.builder()
                .policyName(policy)
                .policyValue(5) // 기존 값
                .build();

        given(policyRepository.findById(policy)).willReturn(Optional.of(entity));

        // when
        pointPolicyService.updatePolicyValue(policy, newValue);

        // then
        assertThat(entity.getPolicyValue()).isEqualTo(newValue); // JPA Dirty Checking으로 엔티티 값이 변경되었는지 확인
    }

    @Test
    @DisplayName("존재하지 않는 정책 수정을 시도할 경우 예외가 발생한다")
    void updatePolicyValue_Fail_NotFound() {
        // given
        PointPolicy policy = PointPolicy.AD_REWARD;
        given(policyRepository.findById(policy)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> pointPolicyService.updatePolicyValue(policy, 100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("지원하지 않는 정책입니다");
    }
}

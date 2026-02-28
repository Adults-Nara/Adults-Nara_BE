package com.ott.core.modules.point.repository;

import com.ott.common.persistence.entity.PointPolicyEntity;
import com.ott.common.persistence.enums.PointPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointPolicyRepository extends JpaRepository<PointPolicyEntity, PointPolicy> {
}

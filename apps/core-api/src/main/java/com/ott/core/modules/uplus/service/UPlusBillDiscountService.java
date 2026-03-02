package com.ott.core.modules.uplus.service;

import com.ott.core.modules.uplus.dto.UPlusSubscriptionDto;
import com.ott.core.modules.uplus.repository.UPlusBillDiscountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UPlusBillDiscountService {

    private final UPlusBillDiscountRepository billDiscountRepository;

    @Transactional(readOnly = true)
    public List<UPlusSubscriptionDto.DiscountHistoryResponse> getHistory(Long userId) {
        return billDiscountRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(UPlusSubscriptionDto.DiscountHistoryResponse::from)
                .toList();
    }
}
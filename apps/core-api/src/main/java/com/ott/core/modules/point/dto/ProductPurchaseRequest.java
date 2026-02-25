package com.ott.core.modules.point.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProductPurchaseRequest {
    private Long orderId;
    private Long productId;
    private Long price;
}

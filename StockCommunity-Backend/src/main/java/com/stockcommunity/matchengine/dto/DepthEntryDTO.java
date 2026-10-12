package com.stockcommunity.matchengine.dto;

import java.math.BigDecimal;

// 獲取五檔中各個價位內單數資訊用的DTO
public record DepthEntryDTO(
        BigDecimal price,
        long totalQuantity) {
    public static DepthEntryDTO create(BigDecimal price, long totalQuantity) {
        return new DepthEntryDTO(price, totalQuantity);
    }
}

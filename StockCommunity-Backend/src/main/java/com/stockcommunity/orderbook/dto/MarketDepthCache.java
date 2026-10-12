package com.stockcommunity.orderbook.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.stockcommunity.matchengine.dto.DepthEntryDTO;

// 五檔數據快照
public record MarketDepthCache(
    List<DepthEntryDTO> buyDepthEntry,
    List<DepthEntryDTO> sellDepthEntry,
    long timestamp
) {
    public static MarketDepthCache create(
        List<DepthEntryDTO> buyDepthEntry,
        List<DepthEntryDTO> sellDepthEntry) {

            return new MarketDepthCache(
                List.copyOf(buyDepthEntry), 
                List.copyOf(sellDepthEntry), 
                Instant.now().toEpochMilli());
        
    }
    
    public BigDecimal getBestBuyPrice() {
        return buyDepthEntry.isEmpty() ? null : buyDepthEntry.get(0).price();
    }

    public BigDecimal getBestAskPrice() {
        return sellDepthEntry.isEmpty() ? null : sellDepthEntry.get(0).price();
    }
}

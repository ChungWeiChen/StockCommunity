package com.stockcommunity.stocks.finmind;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;

// FinMind TaiwanStockPrice dataset 單筆資料，欄位對應官方文件：
// date / stock_id / open / max / min / close / Trading_Volume（還有其他欄位用不到，忽略掉）
@JsonIgnoreProperties(ignoreUnknown = true)
public record FinMindStockPriceDTO(
        LocalDate date,

        @JsonProperty("stock_id")
        String stockId,

        BigDecimal open,
        BigDecimal max,   // FinMind 用 max/min 表示最高/最低價，不是 high/low
        BigDecimal min,
        BigDecimal close,

        @JsonProperty("Trading_Volume")
        Long tradingVolume
) {}
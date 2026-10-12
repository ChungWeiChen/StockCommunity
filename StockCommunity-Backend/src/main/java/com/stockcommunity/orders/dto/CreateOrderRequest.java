package com.stockcommunity.orders.dto;

import lombok.Data;

import java.math.BigDecimal;

import com.stockcommunity.orders.OrderType;
import com.stockcommunity.orders.PriceType;
import com.stockcommunity.orders.TradingTime;
import com.stockcommunity.orders.TradingType;

@Data
public class CreateOrderRequest {
    private String stockCode;
    private OrderType orderType;
    private Integer quantity;
    private BigDecimal wantBuyPrice;      // 限價單才需要填，市價單可以不傳/傳 null
    private BigDecimal protectionPrice;   // 市價買單最高價／市價賣單最低價
    private PriceType priceType;
    private TradingType tradingType;
    private TradingTime tradingTime;
}

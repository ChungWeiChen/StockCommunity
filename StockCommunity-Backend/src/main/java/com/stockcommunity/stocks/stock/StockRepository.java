package com.stockcommunity.stocks.stock;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockRepository extends JpaRepository<Stock, String> {

    List <Stock> findByStockCodeStartingWithOrSimpleNameContaining(String stockCode, String simpleName);

    // 依產業代號查詢股票，給產業別篩選功能用
    List<Stock> findByIndustry(String industry);
}

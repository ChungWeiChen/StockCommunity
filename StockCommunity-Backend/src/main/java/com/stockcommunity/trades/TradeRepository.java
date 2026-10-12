package com.stockcommunity.trades;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TradeRepository extends JpaRepository<Trade, Long> {

	// 找當天該股票最新的一筆交易
	@Query("SELECT t FROM Trade t " +
			"WHERE t.stockCode = :stockCode " +
			"AND CAST(t.tradedAt AS date) = :tradeDate " +
			"ORDER BY t.tradedAt DESC LIMIT 1")
	Optional<Trade> findLatestPriceByStockCodeAndDate(
			@Param("stockCode") String stockCode,
			@Param("tradeDate") LocalDate date);

	// 查某筆買單相關的所有成交紀錄
	List<Trade> findByBuyOrderId(Long buyOrderId);

	// 查某筆賣單相關的所有成交紀錄
	List<Trade> findBySellOrderId(Long sellOrderId);

	// 查某檔股票的所有成交紀錄（之後可能用在「查看某股票近期成交」這類功能）
	List<Trade> findByStockCode(String stockCode);
}

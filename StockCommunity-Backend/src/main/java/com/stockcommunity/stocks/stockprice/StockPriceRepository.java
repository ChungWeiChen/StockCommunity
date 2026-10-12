package com.stockcommunity.stocks.stockprice;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StockPriceRepository extends JpaRepository<StockPrice, Long> {

        // 查某檔股票資料庫裡最新的交易日期，開機補資料前先查這個
        // 判斷這次還需不需要重新呼叫FinMind，避免每次重啟都重打一次全部股票的API
        @Query("SELECT MAX(s.tradeDate) FROM StockPrice s WHERE s.stockCode = :stockCode")
        Optional<LocalDate> findLatestTradeDateByStockCode(@Param("stockCode") String stockCode);

        // 排行榜/未實現損益用：一次撈出「每檔股票」最新一筆的股價資料(不是日期，是整筆StockPrice)，
        // 用子查詢比對每檔股票自己的最新交易日，一次查完，不用對每檔股票各查一次
        @Query("SELECT s FROM StockPrice s WHERE s.tradeDate = " +
                        "(SELECT MAX(s2.tradeDate) FROM StockPrice s2 WHERE s2.stockCode = s.stockCode)")
        List<StockPrice> findLatestPricePerStock();

        // 查某檔股票的所有歷史行情（依日期排序，方便畫走勢圖）
        List<StockPrice> findByStockCodeOrderByTradeDateAsc(String stockCode);

        // 查某檔股票在指定日期區間內的歷史行情（依日期排序，方便畫走勢圖）
        // 給「歷史股價走勢圖」功能用：使用者選股票代號+日期區間，畫出這段區間的價格趨勢
        List<StockPrice> findByStockCodeAndTradeDateBetweenOrderByTradeDateAsc(
                        String stockCode, LocalDate startDate, LocalDate endDate);

        // 查某檔股票、某一天的行情
        // 這個方法之後會被 StockPriceService 用來查「前一個交易日」的資料，計算漲跌幅
        Optional<StockPrice> findByStockCodeAndTradeDate(String stockCode, LocalDate tradeDate);

        // 尋找數個特定股票、交易日小於指定日期，且依日期降序排列的第一筆資料
        // 用於找上一個交易日的收盤價
        @Query("SELECT s.closePrice FROM StockPrice s "
                        + "WHERE s.tradeDate < :tradeDate AND s.stockCode = :stockCode "
                        + "ORDER BY s.tradeDate DESC LIMIT 1")
        Optional<BigDecimal> findT1ClosePrice(
                        @Param("stockCode") String stockCode,
                        @Param("tradeDate") LocalDate date);

        // 【效能優化】IndustryMomentumService 用：一次撈出多檔股票「指定日期之前」最新的收盤價，
        // 取代對每檔股票各呼叫一次 findT1ClosePrice() 的 N+1 寫法
        // ROW_NUMBER() 依 stock_code 分組、按 trade_date 由新到舊排序，取每組第1筆(rn=1)就是前一交易日收盤價
        @Query(value = "SELECT t.stock_code, t.close_price FROM ( " +
                "SELECT stock_code, close_price, " +
                "ROW_NUMBER() OVER (PARTITION BY stock_code ORDER BY trade_date DESC) AS rn " +
                "FROM stock_prices " +
                "WHERE stock_code IN (:stockCodes) AND trade_date < :tradeDate" +
                ") t WHERE t.rn = 1",
                nativeQuery = true)
        List<Object[]> findT1ClosePricesBatch(
                @Param("stockCodes") Collection<String> stockCodes,
                @Param("tradeDate") LocalDate tradeDate);

        // 根據交易日期，只查詢並回傳已存在的股票代碼清單 (Projection 優化)
        @Query("SELECT s.stockCode FROM StockPrice s WHERE s.tradeDate = :tradeDate")
        List<String> findExistingStockCodesByTradeDate(@Param("tradeDate") LocalDate tradeDate);

        // 如果外部 API 的 cleanedList 橫跨多個日期，則改用這個 IN 查詢
        @Query("SELECT s.stockCode FROM StockPrice s WHERE s.tradeDate IN :tradeDates")
        List<String> findExistingStockCodesByTradeDates(@Param("tradeDates") Collection<LocalDate> tradeDates);

        // 【新增】查某檔股票在指定日期區間內，資料庫已經有的交易日期
        // 給 FinMind 補歷史資料用：補資料前先查一次，避免對已存在的 (stockCode, tradeDate) 重複寫入，
        // 撞到 StockPrice 的 @UniqueConstraint(columnNames = {"stockCode", "tradeDate"})
        @Query("SELECT s.tradeDate FROM StockPrice s "
                        + "WHERE s.stockCode = :stockCode AND s.tradeDate BETWEEN :startDate AND :endDate")
        List<LocalDate> findExistingTradeDatesByStockCodeAndRange(
                        @Param("stockCode") String stockCode,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);
}

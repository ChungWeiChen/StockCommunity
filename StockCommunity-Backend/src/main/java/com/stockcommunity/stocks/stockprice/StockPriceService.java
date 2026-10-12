package com.stockcommunity.stocks.stockprice;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class StockPriceService {

    private final StockPriceRepository stockPriceRepository;

    public StockPriceService(StockPriceRepository stockPriceRepository) {
        this.stockPriceRepository = stockPriceRepository;
    }

//    暫不開放
//    // 新增一筆每日行情
//    public StockPrice createStockPrice(
//            String stockCode,
//            LocalDate tradeDate,
//            BigDecimal openPrice,
//            BigDecimal highPrice,
//            BigDecimal lowPrice,
//            BigDecimal closePrice,
//            Long volume) {
//
//        // 防呆：股票代號必須真實存在
//        if (stockRepository.findById(stockCode).isEmpty()) {
//            throw new IllegalArgumentException("找不到股票代號：" + stockCode);
//        }
//
//        // 防呆：四個價格都不能是 null 或負數/0
//        if (openPrice == null || openPrice.compareTo(BigDecimal.ZERO) <= 0
//                || highPrice == null || highPrice.compareTo(BigDecimal.ZERO) <= 0
//                || lowPrice == null || lowPrice.compareTo(BigDecimal.ZERO) <= 0
//                || closePrice == null || closePrice.compareTo(BigDecimal.ZERO) <= 0) {
//            throw new IllegalArgumentException("開高低收價格都必須大於 0");
//        }
//
//        // 防呆：成交量不能是負數（可以是 0，代表當天沒有成交）
//        if (volume == null || volume < 0) {
//            throw new IllegalArgumentException("成交量不能為負數");
//        }
//
//        // 防呆：最高價必須是四個價格裡最大（或並列最大）的那個
//        if (highPrice.compareTo(openPrice) < 0
//                || highPrice.compareTo(lowPrice) < 0
//                || highPrice.compareTo(closePrice) < 0) {
//            throw new IllegalArgumentException("最高價不能低於開盤價、最低價或收盤價");
//        }
//
//        // 防呆：最低價必須是四個價格裡最小（或並列最小）的那個
//        if (lowPrice.compareTo(openPrice) > 0
//                || lowPrice.compareTo(highPrice) > 0
//                || lowPrice.compareTo(closePrice) > 0) {
//            throw new IllegalArgumentException("最低價不能高於開盤價、最高價或收盤價");
//        }
//
//        // 防呆：同一檔股票、同一天不能重複新增行情
//        boolean alreadyExists = stockPriceRepository
//                .findByStockCodeAndTradeDate(stockCode, tradeDate)
//                .isPresent();
//        if (alreadyExists) {
//            throw new IllegalArgumentException("這檔股票在這天已經有行情資料了");
//        }
//
//        StockPrice stockPrice = new StockPrice();
//        stockPrice.setStockCode(stockCode);
//        stockPrice.setTradeDate(tradeDate);
//        stockPrice.setOpenPrice(openPrice);
//        stockPrice.setHighPrice(highPrice);
//        stockPrice.setLowPrice(lowPrice);
//        stockPrice.setClosePrice(closePrice);
//        stockPrice.setVolume(volume);
//        return stockPriceRepository.save(stockPrice);
//    }

    // 查某檔股票的所有歷史行情（依日期排序，畫走勢圖用）
    public List<StockPrice> findPriceHistory(String stockCode) {
        return stockPriceRepository.findByStockCodeOrderByTradeDateAsc(stockCode);
    }

    // 算某檔股票在某一天的漲跌幅
    //
    // 公式：漲跌幅(%) = (今天收盤價 - 前一天收盤價) ÷ 前一天收盤價 × 100
    //
    // 舉例：前一天收盤 100 元，今天收盤 105 元
    //   change = 105 - 100 = 5（漲了 5 元，正數代表上漲，負數代表下跌）
    //   change ÷ 前一天收盤價 = 5 ÷ 100 = 0.05（漲了 5%，還沒轉成百分比格式前的比例）
    //   0.05 × 100 = 5（轉成一般看得懂的百分比數字：5%）
    //
    // 這個方法不會自己「存」漲跌幅，是每次呼叫時，即時查兩天的 StockPrice、即時算出來
    // 好處：不用擔心資料庫裡存的漲跌幅跟實際的開高低收對不上（資料只有一份、不會失真）
    //
    // 回傳 null 代表查不到前一天的資料（例如這是這檔股票最早的一筆紀錄），無法計算漲跌幅
    public BigDecimal calculateChangePercent(String stockCode, LocalDate tradeDate, LocalDate previousDate) {
        // 步驟 1：查「今天」這一筆行情，查不到代表資料本身有問題，直接丟例外
        StockPrice today = stockPriceRepository.findByStockCodeAndTradeDate(stockCode, tradeDate)
                .orElseThrow(() -> new IllegalArgumentException("找不到當日行情"));

        // 步驟 2：查「前一天」這一筆行情，查不到是合理情況（例如剛上市的股票），不當例外處理
        StockPrice previous = stockPriceRepository.findByStockCodeAndTradeDate(stockCode, previousDate)
                .orElse(null);

        // 步驟 3：如果沒有前一天的資料，沒辦法算漲跌幅，直接回傳 null 讓呼叫端自己決定怎麼顯示
        if (previous == null) {
            return null;
        }

        // 步驟 4：算出「今天收盤價 - 前一天收盤價」的價差
        BigDecimal change = today.getClosePrice().subtract(previous.getClosePrice());

        // 步驟 5：價差 ÷ 前一天收盤價，得到「比例」，再乘以 100 轉成百分比數字
        // divide() 除不盡時要指定精度（這裡故意留 4 位小數，比一般金額多留一點，因為百分比常需要更細的精度）跟捨入方式
        return change.divide(previous.getClosePrice(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    // 查某檔股票在指定日期區間的歷史行情（依日期排序），給「歷史股價走勢圖」功能使用
    public List<StockPrice> findPriceHistoryByRange(String stockCode, LocalDate startDate, LocalDate endDate) {

        // 防呆：開始日期不能晚於結束日期，避免使用者把起訖日期填反，
        // 導致查詢條件邏輯錯誤（雖然查得到空結果，但錯誤原因會讓人誤以為是資料庫沒資料）
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("開始日期不能晚於結束日期");
        }

        return stockPriceRepository.findByStockCodeAndTradeDateBetweenOrderByTradeDateAsc(
                stockCode, startDate, endDate);
    }
}

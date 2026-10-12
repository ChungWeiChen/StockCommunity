package com.stockcommunity.stocks.stock;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;

    // 新增一檔股票到主檔（之後串真實資料時，這裡會被排程呼叫取代手動新增）
    public Stock createStock(String stockCode,
            String companyName,
            String simpleName,
            String market,
            String industry,
            Long capital) {

        // 防呆：股票代號不能是空字串
        if (stockCode == null || stockCode.trim().isEmpty()) {
            throw new IllegalArgumentException("股票代號不能為空");
        }

        // 防呆：公司全名不能是空字串
        if (companyName == null || companyName.trim().isEmpty()) {
            throw new IllegalArgumentException("公司全名不能為空");
        }

        // 防呆：股票簡稱不能是空字串
        if (simpleName == null || simpleName.trim().isEmpty()) {
            throw new IllegalArgumentException("股票簡稱不能為空");
        }

        // 防呆：同一個股票代號不能重複新增（stockCode 是主鍵，重複會直接覆蓋掉原本資料）
        if (stockRepository.existsById(stockCode)) {
            throw new IllegalArgumentException("股票代號已存在：" + stockCode);
        }

        Stock stock = new Stock();
        stock.setStockCode(stockCode.trim());
        stock.setCompanyName(companyName.trim());
        stock.setSimpleName(simpleName.trim());
        stock.setMarket(market);
        stock.setIndustry(industry);
        stock.setCapital(capital); // 選填，沒有資料來源時允許 null
        return stockRepository.save(stock);
    }

    // 查詢全部股票主檔
    public List<Stock> findAllStocks() {
        return stockRepository.findAll();
    }

    // 依代號查單一股票，找不到就丟例外
    public Stock findByStockCode(String stockCode) {
        return stockRepository.findById(stockCode)
                .orElseThrow(() -> new IllegalArgumentException("找不到股票代號：" + stockCode));
    }

    // 依關鍵字搜尋股票（代號或簡稱其中一個包含關鍵字就算符合）
    public List<Stock> searchStocks(String keyword) {
        return stockRepository.findByStockCodeStartingWithOrSimpleNameContaining(keyword, keyword);
    }

    // 依產業代號查詢股票
    public List<Stock> findStocksByIndustry(String industry) {
        return stockRepository.findByIndustry(industry);
    }

}

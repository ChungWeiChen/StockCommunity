package com.stockcommunity.news;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockcommunity.news.dto.NewsDTO;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class NewsService {

    private final RestClient restClient = RestClient.create();

    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // 首頁市場焦點的預設股票池
    private static final List<String> DEFAULT_NEWS_STOCKS = List.of(
            "2330", // 台積電
            "2317", // 鴻海
            "2454", // 聯發科
            "2382", // 廣達
            "2308",  // 台達電
            
            "2603", // 長榮－航運
            "2881", // 富邦金－金融
            "2882", // 國泰金－金融
            "2412", // 中華電－電信
            "1301"  // 台塑－傳產
    );


    // 每支股票的新聞快取
    private final Map<String, List<NewsDTO>> cachedNews = new HashMap<>();

    // 每支股票上一次更新新聞的時間
    private final Map<String, LocalDateTime> lastNewsUpdateTime = new HashMap<>();


    public List<NewsDTO> getStockNews(String stockCode) throws Exception {

        // 1. 先檢查這支股票有沒有快取
        List<NewsDTO> cached = cachedNews.get(stockCode);

        LocalDateTime lastUpdate = lastNewsUpdateTime.get(stockCode);

        // 有快取，而且還沒有超過 30 分鐘
        if (cached != null
                && lastUpdate != null
                && lastUpdate.plusMinutes(30)
                             .isAfter(LocalDateTime.now())) {

            System.out.println("=== 使用新聞快取：" + stockCode + " ===");

            return cached;
        }

        // 2. 沒有快取 → 呼叫 FinMind
        System.out.println("=== 呼叫 FinMind API：" + stockCode + " ===");

        LocalDate startDate = LocalDate.now().minusDays(1);

        String response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("api.finmindtrade.com")
                        .path("/api/v4/data")
                        .queryParam("dataset", "TaiwanStockNews")
                        .queryParam("data_id", stockCode)
                        .queryParam("start_date", startDate)
                        .build())
                .retrieve()
                .body(String.class);

        // 3. 解析 FinMind JSON
        JsonNode root = objectMapper.readTree(response);

        JsonNode data = root.get("data");
        
        List<NewsDTO> newsList = new ArrayList<>();

        for (JsonNode news : data) {

            String source = news.get("source").asText();

            // 4. 排除 CMoney
            if (source.equalsIgnoreCase("CMoney")
                    || source.equalsIgnoreCase("cmoney.tw")) {
            	
                continue;
            }

            // 5. 轉成自己的 NewsDTO
            NewsDTO dto = new NewsDTO();

            dto.setStockCode(news.get("stock_id").asText());

            dto.setTitle(news.get("title").asText());

            dto.setUrl(news.get("link").asText());

            dto.setPublishedAt(news.get("date").asText());

            dto.setSourceName(source);

            newsList.add(dto);
        }

        // 6. 最新新聞排前面
        newsList.sort(
                (a, b) ->
                        b.getPublishedAt()
                         .compareTo(a.getPublishedAt())
        );

        // 7. 存進這支股票自己的快取
        cachedNews.put( stockCode, newsList);

        lastNewsUpdateTime.put(stockCode,LocalDateTime.now());
        

        return newsList;
    }
    
    
    public List<NewsDTO> getMarketNews() throws Exception {

        // 【9/26 WG 加註】原本這裡是用for迴圈依序呼叫5支股票的新聞，
        // 快取一過期(30分鐘)或後端剛重啟(快取只存在記憶體，重啟就會被清空)，
        // 5支股票會依序打外部FinMind API，時間加總起來很容易超過前端5秒逾時，
        // 造成「後端剛重啟後，第一次進社群頁就報錯、整站被404畫面蓋掉」。
        // 改成5支股票平行呼叫，等待時間變成「最慢那一支」而不是「5支加起來」。
        //
        // 舊寫法(保留參考)：
        // List<NewsDTO> marketNews = new ArrayList<>();
        // for (String stockCode : DEFAULT_NEWS_STOCKS) {
        //     List<NewsDTO> stockNews = getStockNews(stockCode);
        //     marketNews.addAll(stockNews.stream().limit(5).toList());
        // }

        // 5支股票同時送出去，不用一支一支等
        List<CompletableFuture<List<NewsDTO>>> futures = DEFAULT_NEWS_STOCKS.stream()
                .map(stockCode -> CompletableFuture.supplyAsync(() -> {
                    try {
                        return getStockNews(stockCode);
                    } catch (Exception e) {
                        // 單一股票新聞抓取失敗，不要讓整個市場新聞API跟著失敗，回傳空清單就好
                        System.out.println("=== 新聞抓取失敗，略過：" + stockCode + " ===");
                        
                        return List.<NewsDTO>of();
                    }
                }))
                .toList();

        // 存放股票池內所有股票的新聞
        List<NewsDTO> marketNews = new ArrayList<>();

        for (CompletableFuture<List<NewsDTO>> future : futures) {

            // 每支股票先取最新 5 則
            marketNews.addAll(
                    future.join().stream()
                            .limit(5)
                            .toList()
            );
        }


        // 最新 → 最舊
        marketNews.sort(
                (a, b) ->
                        b.getPublishedAt()
                                .compareTo(a.getPublishedAt())
        );


        // 去除重複新聞
        Map<String, NewsDTO> uniqueNews = new LinkedHashMap<>();

        for (NewsDTO news : marketNews) {

            String title = news.getTitle();

            // 去掉標題最後面的來源
            String normalizedTitle =
                    title.replaceFirst("\\s*-\\s*[^-]+$", "")
                            .trim();

            uniqueNews.putIfAbsent(normalizedTitle, news);
        }


        // 最後只取最新 10 則
        return uniqueNews.values()
                .stream()
                .limit(15)
                .toList();
    }
    
  

    
}

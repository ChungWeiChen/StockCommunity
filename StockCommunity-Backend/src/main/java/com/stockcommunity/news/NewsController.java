package com.stockcommunity.news;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.stockcommunity.news.dto.NewsDTO;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
public class NewsController {

    private final NewsService newsService;

    @GetMapping("/api/news/stock/{stockCode}")
    public ResponseEntity<List<NewsDTO>> getStockNews(
            @PathVariable String stockCode) throws Exception {

        List<NewsDTO> news = newsService.getStockNews(stockCode);

        return ResponseEntity.ok(news);
    }
    
    @GetMapping("/api/news")
    public ResponseEntity<List<NewsDTO>> getMarketNews()
            throws Exception {

        List<NewsDTO> news = newsService.getMarketNews();

        return ResponseEntity.ok(news);
    }
    
    
 
}
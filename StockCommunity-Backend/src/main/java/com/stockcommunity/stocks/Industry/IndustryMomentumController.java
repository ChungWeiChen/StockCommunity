package com.stockcommunity.stocks.Industry;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.stockcommunity.stocks.dto.IndustryMomentumResult;

import lombok.RequiredArgsConstructor;

// 今日領漲/領跌產業，給社群首頁市場焦點卡片用
@RestController
@RequestMapping("/api/industries")
@RequiredArgsConstructor
public class IndustryMomentumController {

    private final IndustryMomentumService industryMomentumService;

    // GET /api/industries/momentum?topN=5，topN沒帶的話預設抓前5名
    @GetMapping("/momentum")
    public ResponseEntity<IndustryMomentumResult> getIndustryMomentum(
            @RequestParam(defaultValue = "5") int topN) {
        return ResponseEntity.ok(industryMomentumService.calculateIndustryMomentum(topN));
    }
}
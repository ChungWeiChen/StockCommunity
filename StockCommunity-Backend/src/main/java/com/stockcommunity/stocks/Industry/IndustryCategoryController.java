package com.stockcommunity.stocks.Industry;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/industries")
public class IndustryCategoryController {

    private final IndustryCategoryService industryCategoryService;

    public IndustryCategoryController(IndustryCategoryService industryCategoryService) {
        this.industryCategoryService = industryCategoryService;
    }

    // 查詢全部產業別清單，前端下拉選單用這支
    @GetMapping
    public ResponseEntity<List<IndustryCategory>> getAllIndustries() {
        return ResponseEntity.ok(industryCategoryService.findAllIndustries());
    }
}
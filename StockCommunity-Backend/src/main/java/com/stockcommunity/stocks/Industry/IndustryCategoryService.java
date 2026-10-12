package com.stockcommunity.stocks.Industry;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IndustryCategoryService {

    private final IndustryCategoryRepository repository;

    public IndustryCategoryService(IndustryCategoryRepository repository) {
        this.repository = repository;
    }

    // 查詢全部產業別，給前端下拉選單用
    public List<IndustryCategory> findAllIndustries() {
        return repository.findAll();
    }
}
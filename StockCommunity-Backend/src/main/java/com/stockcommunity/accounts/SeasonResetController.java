package com.stockcommunity.accounts;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/season")
@RequiredArgsConstructor
public class SeasonResetController {

    private final SeasonResetService seasonResetService;

    // 測試/展示用：不用等到下週一，手動觸發一次
    @PostMapping("/reset")
    public ResponseEntity<String> triggerReset() {
        seasonResetService.resetSeason();
        return ResponseEntity.ok("賽季重置完成");
    }
}
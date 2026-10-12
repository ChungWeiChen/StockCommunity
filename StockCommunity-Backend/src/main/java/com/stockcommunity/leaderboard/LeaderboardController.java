package com.stockcommunity.leaderboard;

import com.stockcommunity.leaderboard.dto.LeaderboardEntryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leaderboard")
@RequiredArgsConstructor
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    // GET /api/leaderboard?limit=10 → 沒帶limit參數時預設抓前10名
    @GetMapping
    public List<LeaderboardEntryDTO> getLeaderboard(
            @RequestParam(defaultValue = "10") int limit) {
        return leaderboardService.getTotalAssetsLeaderboard(limit); // 9/29改：呼叫改名後的方法
    }
}
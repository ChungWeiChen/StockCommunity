package com.stockcommunity.leaderboard;

import com.stockcommunity.accounts.TradingAccount;
import com.stockcommunity.accounts.TradingAccountRepository;
import com.stockcommunity.common.SystemAccounts;
import com.stockcommunity.holdings.Holding;
import com.stockcommunity.holdings.HoldingRepository;
import com.stockcommunity.leaderboard.dto.LeaderboardEntryDTO;
import com.stockcommunity.stocks.stockprice.StockPrice;
import com.stockcommunity.stocks.stockprice.StockPriceRepository;
import com.stockcommunity.users.Users;
import com.stockcommunity.users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeaderboardService {

    private final HoldingRepository holdingRepository;
    private final StockPriceRepository stockPriceRepository;
    private final UserRepository userRepository;
    private final TradingAccountRepository tradingAccountRepository; // 新增：算現金餘額要用交易帳戶資料

    // topN：業界常見寫法，意思是「取前N名」，這裡指要回傳排行榜前幾名
    // 9/29改：排行榜依據從「未實現損益」改成「總資產」，方法名稱同步改成 getTotalAssetsLeaderboard
    public List<LeaderboardEntryDTO> getTotalAssetsLeaderboard(int topN) {
        // 第1步：拿全部交易帳戶(排除系統帳號)，先把每個人的「現金」部分算好當底
        // 現金 = availableCash(可用) + frozenCash(掛單凍結)，凍結中的錢還是使用者的錢，
        // 只是暫時不能動用，不能因為掛了一張單就讓總資產看起來變少
        List<TradingAccount> accounts = tradingAccountRepository.findAllExcludingUsernames(SystemAccounts.RESERVED_USERNAMES);
        Map<Long, BigDecimal> totalAssetsByUser = new HashMap<>();
        for (TradingAccount account : accounts) {
            BigDecimal cash = account.getAvailableCash().add(account.getFrozenCash());
            totalAssetsByUser.put(account.getUserId(), cash);
        }

        // 第2步：拿全部持股(排除系統帳號)
        List<Holding> holdings = holdingRepository.findAllExcludingUsernames(SystemAccounts.RESERVED_USERNAMES);

        // 第3步：拿每檔股票的現價，轉成 Map 方便查
        Map<String, BigDecimal> latestPriceMap = stockPriceRepository.findLatestPricePerStock().stream()
                .collect(Collectors.toMap(StockPrice::getStockCode, StockPrice::getClosePrice));

        // 第4步：把每個人的持股市值，疊加到第1步已經有現金底的 totalAssetsByUser 上
        // 原本(未實現損益版本)算的是 (現價-平均成本)×股數；總資產只需要市值，
        // 所以改成 現價×股數，不用再減平均成本
        for (Holding h : holdings) {
            BigDecimal currentPrice = latestPriceMap.get(h.getStockCode());
            if (currentPrice == null) continue; // 防呆：這檔股票完全沒有股價資料就跳過，不要讓NPE炸掉整個排行榜
            BigDecimal marketValue = currentPrice.multiply(BigDecimal.valueOf(h.getQuantity()));
            // 用merge而不是put：這裡userId理論上第1步已經存在(有持股必有交易帳戶)，
            // merge只是多一層防呆，萬一真的資料不一致也不會漏算
            totalAssetsByUser.merge(h.getUserId(), marketValue, BigDecimal::add);
        }

        // 第5步：排序(由高到低)、取前N名、補上使用者名稱後回傳
        return totalAssetsByUser.entrySet().stream()
                .sorted(Map.Entry.<Long, BigDecimal>comparingByValue().reversed())
                .limit(topN)
                .map(entry -> {
                    Users user = userRepository.findById(entry.getKey()).orElse(null);
                    String displayName = (user != null) ? user.getUsername() : "未知使用者";
                    return new LeaderboardEntryDTO(entry.getKey(), displayName, entry.getValue());
                })
                .collect(Collectors.toList());
    }
}
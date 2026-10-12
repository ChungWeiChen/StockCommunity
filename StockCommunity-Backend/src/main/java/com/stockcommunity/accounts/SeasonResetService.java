package com.stockcommunity.accounts;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.stockcommunity.common.SystemAccounts;
import com.stockcommunity.holdings.Holding;
import com.stockcommunity.holdings.HoldingRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SeasonResetService {

    private final TradingAccountRepository accountRepository;
    private final CashLedgerRepository ledgerRepository;
    private final HoldingRepository holdingRepository;

    // 每週一凌晨0點觸發
    // 先註解掉，不讓他自動跑；等要正式測試/上線時再打開
    // @Scheduled(cron = "0 0 0 * * MON")
    @Transactional
    public void resetSeason() {
        // 用日期當這次重置的識別碼，防止同一天重複觸發時重複入帳
        String seasonKey = "SEASON_RESET:" + LocalDate.now();

        List<TradingAccount> accounts =
                accountRepository.findAllExcludingUsernames(SystemAccounts.RESERVED_USERNAMES);

        for (TradingAccount account : accounts) {
            String referenceKey = seasonKey + ":USER:" + account.getUserId();
            if (ledgerRepository.existsByReferenceKey(referenceKey)) {
                continue;
            }

            BigDecimal availableDelta = account.getInitialCash().subtract(account.getAvailableCash());
            BigDecimal frozenDelta = account.getFrozenCash().negate();

            account.setAvailableCash(account.getInitialCash());
            account.setFrozenCash(BigDecimal.ZERO);
            accountRepository.save(account);

            ledgerRepository.save(new CashLedger(
                    account.getUserId(), null, null, CashLedgerType.SEASON_RESET,
                    availableDelta, frozenDelta,
                    account.getAvailableCash(), account.getFrozenCash(),
                    referenceKey, "每週賽季重置：資金回復初始金額"));
        }

        // 連同holdings一起清空（跟OOVE/Wei手動測試時做的事一樣，只是自動化）
        List<Holding> holdings =
                holdingRepository.findAllExcludingUsernames(SystemAccounts.RESERVED_USERNAMES);
        holdingRepository.deleteAll(holdings);
    }
}
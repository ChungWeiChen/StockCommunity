package com.stockcommunity.accounts;

import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.stockcommunity.accounts.dto.CashLedgerResponse;
import com.stockcommunity.accounts.dto.TradingAccountResponse;
import com.stockcommunity.security.CurrentUserProvider;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/trading-accounts")
@RequiredArgsConstructor
public class TradingAccountController {

    private final TradingAccountService accountService;
    private final CurrentUserProvider currentUserProvider;

    // 當前登入user的交易帳戶查詢
    @GetMapping("/me")
    public ResponseEntity<TradingAccountResponse> getMyAccount() {
        Long userId = currentUserProvider.getCurrentUserId();
        TradingAccount account = accountService.getMyAccount(userId);
        return ResponseEntity.ok(TradingAccountResponse.from(account));
    }

    // 查詢當前登入者的資金流水，預設每頁 20 筆、最新紀錄優先
    @GetMapping("/me/ledger")
    public ResponseEntity<Page<CashLedgerResponse>> getMyLedger(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = currentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(accountService.getLedger(userId, page, size));
    }
}

package com.stockcommunity.accounts;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.stockcommunity.accounts.dto.CashLedgerResponse;
import com.stockcommunity.orders.Order;
import com.stockcommunity.trades.Trade;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TradingAccountService {

    private final TradingAccountRepository accountRepository;
    private final CashLedgerRepository ledgerRepository;

    // 在設定application.properties 有放初始化金額
    @Value("${trading.initial-cash:1000000.00}")
    private BigDecimal initialCash;
    // 在設定application.properties 有放初始化金額 (這是造市者用的系統帳號金額)
    @Value("${trading.system-account.initial-cash:1000000000.00}")
    private BigDecimal systemAccountInitialCash;

    // 沒帳戶才建立
    // 已經有帳戶就直接回傳
    // 重啟後不會再次發放 100 萬
    @Transactional
    public TradingAccount createAccountIfAbsent(Long userId, boolean systemAccount) {
        TradingAccount account = accountRepository.findByUserId(userId)
                .orElseGet(() -> createAccount(userId, systemAccount));
        // 首次建立會送出一個建立的流水帳
        createInitialDepositLedgerIfAbsent(account);
        return account;
    }

    private TradingAccount createAccount(Long userId, boolean systemAccount) {
        BigDecimal startingCash = systemAccount
                ? systemAccountInitialCash
                : initialCash;

        TradingAccount account = new TradingAccount();
        account.setUserId(userId);
        account.setInitialCash(startingCash);
        account.setAvailableCash(startingCash);
        account.setFrozenCash(BigDecimal.ZERO);

        return accountRepository.save(account);
    }

    // 建立流水帳
    private void createInitialDepositLedgerIfAbsent(TradingAccount account) {
        String referenceKey = "INITIAL_DEPOSIT:USER:" + account.getUserId();
        // 比對流水帳，已存在就不重複新增
        if (ledgerRepository.existsByReferenceKey(referenceKey)) {
            return;
        }

        CashLedger ledger = new CashLedger(
                account.getUserId(),
                null,
                null,
                CashLedgerType.INITIAL_DEPOSIT,
                account.getInitialCash(),
                BigDecimal.ZERO,
                account.getInitialCash(),
                BigDecimal.ZERO,
                referenceKey,
                "發放模擬交易起始資金"
        );
        ledgerRepository.save(ledger);
    }

    @Transactional
    public TradingAccount getMyAccount(Long userId) {
        return accountRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("找不到模擬交易帳戶"));
    }

    @Transactional
    public Page<CashLedgerResponse> getLedger(Long userId, int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("頁碼不能小於 0");
        }
        if (size <= 0 || size > 100) {
            throw new IllegalArgumentException("每頁筆數必須介於 1 到 100");
        }

        getMyAccount(userId);
        return ledgerRepository
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
                .map(CashLedgerResponse::from);
    }
    // 凍結資金方法
    @Transactional
    public void reserveCash(Order order, BigDecimal amount) {
        // 加入refkey後,用來比對避免重複新增交易流水帳
        String referenceKey = "ORDER_RESERVE:ORDER:" + order.getId();
        if (ledgerRepository.existsByReferenceKey(referenceKey)) {
            return;
        }
        // 找到此orderId對應的交易帳戶
        TradingAccount account = getAccountForUpdate(order.getUserId());
        // 交易帳戶驗證
        account.reserveCash(amount);
        // order端加上此次凍結的資金
        order.setReservedCash(amount);
        // 交易帳戶流水帳
        saveLedger(
                order.getUserId(), order.getId(), null, CashLedgerType.ORDER_RESERVE,
                amount.negate(), amount, account, referenceKey, "買進委託凍結資金");
    }
    // 買進成交結算
    @Transactional
    public void settleBuyTrade(Order order, Trade trade) {
        // 加入refkey後,用來比對避免重複新增交易流水帳
        String referenceKey = "BUY_SETTLEMENT:TRADE:" + trade.getId() + ":USER:" + order.getUserId();
        if (ledgerRepository.existsByReferenceKey(referenceKey)) {
            return;
        }
        // 依據限價單或是市價單 取用WantBuyPrice(限價單) or ProtectionPrice(市價單)
        BigDecimal reservationPrice = order.getPriceType() == com.stockcommunity.orders.PriceType.LIMIT_ORDER
                ? order.getWantBuyPrice()
                : order.getProtectionPrice();
        // 本次需凍結的資金 = 數量 x 金額
        BigDecimal reservedForTrade = money(reservationPrice.multiply(BigDecimal.valueOf(trade.getQuantity())));
        // 實際成交金額
        BigDecimal tradeAmount = money(trade.getPrice().multiply(BigDecimal.valueOf(trade.getQuantity())));
        // 此order凍結的資金
        BigDecimal orderReserved = order.getReservedCash() == null ? BigDecimal.ZERO : order.getReservedCash();

        if (orderReserved.compareTo(reservedForTrade) < 0) {
            throw new IllegalStateException("買單凍結資金不足，無法完成成交");
        }

        TradingAccount account = getAccountForUpdate(order.getUserId());
        // 成交計算
        BigDecimal refund = account.settleBuy(reservedForTrade, tradeAmount);

        order.setReservedCash(orderReserved.subtract(reservedForTrade));

        saveLedger(
                order.getUserId(), order.getId(), trade.getId(), CashLedgerType.BUY_SETTLEMENT,
                refund, reservedForTrade.negate(), account, referenceKey, "買進成交結算（含價差退回）");
    }

    // 賣出後金額處理
    @Transactional
    public void creditSellTrade(Order order, Trade trade) {
        String referenceKey = "SELL_PROCEEDS:TRADE:" + trade.getId() + ":USER:" + order.getUserId();
        if (ledgerRepository.existsByReferenceKey(referenceKey)) {
            return;
        }

        BigDecimal tradeAmount = money(trade.getPrice().multiply(BigDecimal.valueOf(trade.getQuantity())));
        TradingAccount account = getAccountForUpdate(order.getUserId());
        account.creditCash(tradeAmount);

        saveLedger(
                order.getUserId(), order.getId(), trade.getId(), CashLedgerType.SELL_PROCEEDS,
                tradeAmount, BigDecimal.ZERO, account, referenceKey, "賣出成交款入帳");
    }

    @Transactional
    public void releaseReservedCash(Order order, String description) {
        BigDecimal amount = order.getReservedCash() == null ? BigDecimal.ZERO : order.getReservedCash();
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        String referenceKey = "ORDER_RELEASE:ORDER:" + order.getId();
        if (ledgerRepository.existsByReferenceKey(referenceKey)) {
            order.setReservedCash(BigDecimal.ZERO);
            return;
        }

        TradingAccount account = getAccountForUpdate(order.getUserId());
        account.releaseCash(amount);
        order.setReservedCash(BigDecimal.ZERO);

        saveLedger(
                order.getUserId(), order.getId(), null, CashLedgerType.ORDER_RELEASE,
                amount, amount.negate(), account, referenceKey, description);
    }

    private TradingAccount getAccountForUpdate(Long userId) {
        return accountRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new IllegalArgumentException("找不到模擬交易帳戶"));
    }

    private void saveLedger(
            Long userId,
            Long orderId,
            Long tradeId,
            CashLedgerType type,
            BigDecimal availableDelta,
            BigDecimal frozenDelta,
            TradingAccount account,
            String referenceKey,
            String description) {
        ledgerRepository.save(new CashLedger(
                userId,
                orderId,
                tradeId,
                type,
                availableDelta,
                frozenDelta,
                account.getAvailableCash(),
                account.getFrozenCash(),
                referenceKey,
                description));
    }

    // 限制數量回傳小數第二位,且四捨五入
    private BigDecimal money(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}

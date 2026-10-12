package com.stockcommunity.init;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.stockcommunity.accounts.TradingAccountService;
import com.stockcommunity.common.SystemAccounts;
import com.stockcommunity.users.UserRepository;
import com.stockcommunity.users.Users;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
// 既有會員補建交易帳戶 ( 不用的話可以取消)
@Slf4j
@Component
@RequiredArgsConstructor
public class TradingAccountInitializer {

    private final UserRepository userRepository;
    private final TradingAccountService accountService;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeExistingAccounts() {
        int checkedAccounts = 0;

        for (Users user : userRepository.findAll()) {
            boolean systemAccount = SystemAccounts.RESERVED_USERNAMES.contains(user.getUsername());
            accountService.createAccountIfAbsent(user.getId(), systemAccount);
            checkedAccounts++;
        }

        log.info("模擬交易帳戶初始化完成，共檢查 {} 位使用者", checkedAccounts);
    }
}

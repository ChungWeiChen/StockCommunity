package com.stockcommunity.common;

import java.util.Set;

public class SystemAccounts {
    // 目前只有造市商帳號，之後做假 AI 帳號時把新帳號的username也加進這個Set就好
    // 排行榜/搜尋/任何要排除系統帳號的查詢都共用這一份名單
    public static final Set<String> RESERVED_USERNAMES = Set.of("SYSTEM_MARKET_MAKER");
}
package com.stockcommunity.marketmaker;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

// 造市者的基礎設定:讀取設定檔中的設定後直接注入
// 不確定該不該塞進configs資料夾

@Component
@ConfigurationProperties(prefix = "market-maker")
@Getter
@Setter
public class MarketMakerProperties {
	private List<String> stocks = new ArrayList<>();

    private int initialHolding;
}

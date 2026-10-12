package com.stockcommunity.stocks.stock;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "stocks")
@Getter
@Setter
@NoArgsConstructor
public class Stock {

    @Id
    @Column(length = 10)
    private String stockCode;   // 股票代號，例如 "2330"，自然鍵，不用流水號 id

    @Column(nullable = false)
    private String companyName;   // 公司法定全名，例如 "台灣積體電路製造股份有限公司"

    @Column(nullable = false)
    private String simpleName;    // 股票簡稱，例如 "台積電"，畫面顯示用；取代原本語意模糊的 stockName

    private String market;      // 上市 / 上櫃

    private String industry;    // 產業別，例如 "半導體業"

    private Long capital;       // 股本(實收資本額)，不是市值；來源資料還沒接上前可以是 null

}

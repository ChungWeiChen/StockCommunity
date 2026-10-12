package com.stockcommunity.marketmaker.dto;

import java.math.BigDecimal;

// 確認那些訂單要送
public record MakerOrderSpec(
	boolean buy,
	boolean sell,
	Integer buyQuantity,
	Integer sellQuantity,
	Integer time,
	BigDecimal spread
	
) {
	public static MakerOrderSpec create(
			boolean buy,
			boolean sell,
			Integer buyQuantity,
			Integer sellQuantity,
			Integer time,
			BigDecimal spread) {
		return new MakerOrderSpec(
				buy,
				sell,
				buyQuantity,
				sellQuantity,
				time,
				spread);
	}
}

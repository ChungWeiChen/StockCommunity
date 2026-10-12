package com.stockcommunity.holdings;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "holdings",
		// 同一個使用者對同一檔股票只會有一筆持股紀錄，不會出現兩筆
		// 資料庫層級強制擋下重複組合，即使程式邏輯之後不小心寫錯也不會產生髒資料
		uniqueConstraints = @UniqueConstraint(columnNames = { "userId", "stockCode" }))
@Getter
@Setter
@NoArgsConstructor
public class Holding {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long userId; // 對應 users.id

	@Column(nullable = false)
	private String stockCode; // 股票代號

	@Column(nullable = false)
	private Integer quantity; // 目前持有股數

	// 已被存活賣單占用的股數；舊資料允許為 null，程式會視為 0
	private Integer reservedQuantity;

	@Column(nullable = false)
	private BigDecimal averagePrice; // 平均每股成本價

	private LocalDateTime updatedAt;

	@PrePersist
	protected void onCreate() {
		if (this.updatedAt == null) {
			this.updatedAt = LocalDateTime.now();
		}
		if(this.quantity == null) {
			this.quantity = 0;
		}
		if(this.reservedQuantity == null) {
			this.reservedQuantity = 0;
		}
		if(this.averagePrice == null) {
			// 原本這裡寫成 this.quantity = 0，是複製上面那段時忘記改變數名，
			// 導致 averagePrice 為 null 時完全沒有被賦值，
			// 存進資料庫時會因為 @Column(nullable = false) 直接報 NOT NULL 錯誤
			// this.quantity = 0;
			this.averagePrice = BigDecimal.ZERO;
		}
	}

	@PreUpdate
	protected void onUpdate() {
		if (this.updatedAt == null) {
			this.updatedAt = LocalDateTime.now();
		}
		
	}

	// 買進後重新計算新的成本價 同時更新股數
	// 
	public void RecalculateAveragePriceAndQuantity(
        Integer newQuantity, 
        BigDecimal price, 
        boolean isBuy) {
		// 1. 如果股票交易數量小於等於 0，直接不處理
		// 1. 基本防呆：數量為空或小於等於 0 直接不處理
		if (newQuantity == null || newQuantity <= 0) {
			return;
		}

		int oldQuantity = (this.quantity != null) ? this.quantity : 0;
		BigDecimal oldAveragePrice = (this.averagePrice != null) ? this.averagePrice : BigDecimal.ZERO;

		if (isBuy) {
				// ========== 買進邏輯：需要重新計算均價與總股數 ==========
				BigDecimal oldQty = BigDecimal.valueOf(oldQuantity);
				BigDecimal newQty = BigDecimal.valueOf(newQuantity);
				BigDecimal totalQty = oldQty.add(newQty); // 新總股數

				// 計算總成本 = (舊股數 * 舊均價) + (新股數 * 本次價格)
				BigDecimal oldTotalCost = oldQty.multiply(oldAveragePrice);
				BigDecimal newCost = newQty.multiply(price);
				BigDecimal totalCost = oldTotalCost.add(newCost);

				// 更新實體數據
				this.quantity = oldQuantity + newQuantity;
				// 四捨五入到小數點後 2 位
				this.averagePrice = totalCost.divide(totalQty, 2, RoundingMode.HALF_UP);

			} else {
				// ========== 賣出邏輯：只扣減股數，不改變均價 ==========
				// 預留防呆：若允許在此拋出異常，可取消註解
				// 先不管他，另外到時候賣單holding不足應該直接在order下單時就告知
				
				// if (newQuantity > oldQuantity) {
				// 	throw new IllegalArgumentException(
				// 			"持股數量不足，無法賣出：目前持有 " + oldQuantity + " 股，本次賣出 " + newQuantity + " 股");
				// }

				settleReservedSale(newQuantity);

				// 持股歸零時，均價也一併歸零，避免殘留數據
				if (this.quantity == 0) {
					this.averagePrice = BigDecimal.ZERO;
				}
			}
	    

	}

	// 計算當前可以被賣出的數量
	public int getAvailableQuantity() {
		int total = this.quantity == null ? 0 : this.quantity;
		int reserved = this.reservedQuantity == null ? 0 : this.reservedQuantity;
		return total - reserved;
	}
	// 下單凍結方法
	public void reserveQuantity(int amount) {
		if (amount <= 0) {
			throw new IllegalArgumentException("凍結股數必須大於 0");
		}
		if (getAvailableQuantity() < amount) {
			throw new IllegalStateException("可賣股數不足，目前可賣：" + getAvailableQuantity());
		}
		this.reservedQuantity = (this.reservedQuantity == null ? 0 : this.reservedQuantity) + amount;
	}
	// 取消返還凍結方法
	public void releaseQuantity(int amount) {
		int reserved = this.reservedQuantity == null ? 0 : this.reservedQuantity;
		if (amount <= 0 || reserved < amount) {
			throw new IllegalStateException("欲解凍股數超過目前凍結股數");
		}
		this.reservedQuantity = reserved - amount;
	}

	// 成交凍結計算方法
	public void settleReservedSale(int amount) {
		int total = this.quantity == null ? 0 : this.quantity;
		int reserved = this.reservedQuantity == null ? 0 : this.reservedQuantity;
		if (amount <= 0 || total < amount || reserved < amount) {
			throw new IllegalStateException("持股或凍結股數不足，無法完成賣出成交");
		}
		this.quantity = total - amount;
		this.reservedQuantity = reserved - amount;
	}

	/**
	 * 取得當前總持有成本（不寫入資料庫，純計算）
	 */
	public BigDecimal getTotalCost() {
		if (this.quantity == null  || this.averagePrice == null || this.quantity <= 0 ) {
			return BigDecimal.ZERO;
		}
		return BigDecimal.valueOf(this.quantity).multiply(this.averagePrice);
	}
	
}

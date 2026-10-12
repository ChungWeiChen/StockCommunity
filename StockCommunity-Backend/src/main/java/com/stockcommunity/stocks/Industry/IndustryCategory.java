package com.stockcommunity.stocks.Industry;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "industry_category")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IndustryCategory {
	@Id
	private String id;          // 產業代號 
	
	private String name;        // 產業中文名稱 

}

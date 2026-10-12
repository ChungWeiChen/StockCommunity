package com.stockcommunity.util;


import java.time.LocalDate;

import java.time.chrono.MinguoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

import java.util.Locale;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

// 民國年格式轉化器
public class MinguoLocalDateDeserializer extends ValueDeserializer<LocalDate> {
	// 建立一個能讀取 3 位數民國年的格式化器 (例如: 1150828)
	private static final DateTimeFormatter MINGUO_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("yyyMMdd")
            .toFormatter(Locale.TAIWAN)
            .withChronology(MinguoChronology.INSTANCE);

    @Override
    public LocalDate deserialize(JsonParser parser, DeserializationContext context) 
            throws JacksonException {
        
        // 1. 取得 JSON 欄位中的字串值 (如 "1150828")
        String minguoDateStr = parser.getValueAsString();
        
        if (minguoDateStr == null || minguoDateStr.isBlank()) {
            return null;
        }

        // 2. 將其解析為 MinguoDate，並自動轉換為西元 LocalDate (例如 2026-08-28)
        return LocalDate.parse(minguoDateStr, MINGUO_FORMATTER);
    }
}
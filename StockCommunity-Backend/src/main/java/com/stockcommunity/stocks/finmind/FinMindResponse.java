package com.stockcommunity.stocks.finmind;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FinMindResponse<T>(
        int status,
        String msg,
        List<T> data
) {}
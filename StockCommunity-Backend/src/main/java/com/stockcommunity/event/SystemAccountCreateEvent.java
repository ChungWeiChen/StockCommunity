package com.stockcommunity.event;

import lombok.Getter;

@Getter
public class SystemAccountCreateEvent {
	private final String message;
    private final long timestamp;

    public SystemAccountCreateEvent(String message) {
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }
}

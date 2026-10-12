package com.stockcommunity.exception;

/**
 * 股票撮合系統專用的自訂異常
 */
public class MatchException extends RuntimeException {
    
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	
	public MatchException(String message) {
        super(message);
    }

    public MatchException(String message, Throwable cause) {
        super(message, cause);
    }
}

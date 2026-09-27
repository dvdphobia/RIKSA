package com.riksa.market.service;

import org.springframework.http.HttpStatus;

public class MarketException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public MarketException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}

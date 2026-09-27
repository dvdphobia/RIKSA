package com.riksa.market.service;

import com.riksa.market.dto.MarketErrorResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MarketExceptionHandler {

    @ExceptionHandler(MarketException.class)
    public ResponseEntity<MarketErrorResponse> handleMarketException(MarketException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(new MarketErrorResponse(exception.getCode(), exception.getMessage()));
    }
}

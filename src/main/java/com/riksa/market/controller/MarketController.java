package com.riksa.market.controller;

import com.riksa.common.constants.MarketEndpoints;
import com.riksa.market.dto.MarketItemsResponse;
import com.riksa.market.dto.PurchaseResponse;
import com.riksa.market.entity.MarketSlot;
import com.riksa.market.service.MarketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(MarketEndpoints.BASE)
@RequiredArgsConstructor
public class MarketController {

    private final MarketService marketService;

    @GetMapping(MarketEndpoints.ITEMS)
    public ResponseEntity<MarketItemsResponse> listItems(@RequestParam(required = false) MarketSlot slot) {
        return ResponseEntity.ok(marketService.listItems(currentUserId(), slot));
    }

    @PostMapping(MarketEndpoints.ITEMS + MarketEndpoints.PURCHASE)
    public ResponseEntity<PurchaseResponse> purchase(
            @PathVariable UUID id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return ResponseEntity.ok(marketService.purchase(currentUserId(), id, idempotencyKey));
    }

    private UUID currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return UUID.fromString(authentication.getName());
    }
}

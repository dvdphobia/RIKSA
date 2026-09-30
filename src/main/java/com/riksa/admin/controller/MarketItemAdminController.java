package com.riksa.admin.controller;

import com.riksa.admin.dto.AdminMarketItem;
import com.riksa.admin.dto.AdminMarketItemRequest;
import com.riksa.admin.service.MarketItemAdminService;
import com.riksa.common.constants.AdminEndpoints;
import com.riksa.common.security.SecurityUtils;
import com.riksa.market.entity.MarketSlot;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(AdminEndpoints.BASE + AdminEndpoints.ADMIN_MARKET_ITEMS)
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class MarketItemAdminController {

    private final MarketItemAdminService marketItemAdminService;

    @GetMapping
    public ResponseEntity<List<AdminMarketItem>> list(
            @RequestParam(required = false) MarketSlot slot,
            @RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(marketItemAdminService.list(slot, active));
    }

    @PostMapping
    public ResponseEntity<AdminMarketItem> create(@RequestBody @Valid AdminMarketItemRequest request) {
        AdminMarketItem response = marketItemAdminService.create(SecurityUtils.currentUserId(), request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path(AdminEndpoints.ADMIN_MARKET_ITEM_BY_ID)
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping(AdminEndpoints.ADMIN_MARKET_ITEM_BY_ID)
    public ResponseEntity<AdminMarketItem> update(
            @PathVariable UUID id,
            @RequestBody @Valid AdminMarketItemRequest request) {
        return ResponseEntity.ok(marketItemAdminService.update(SecurityUtils.currentUserId(), id, request));
    }

    @DeleteMapping(AdminEndpoints.ADMIN_MARKET_ITEM_BY_ID)
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        MarketItemAdminService.DeleteResult result = marketItemAdminService.delete(SecurityUtils.currentUserId(), id);
        if (result.deactivated()) return ResponseEntity.ok(result.item());
        return ResponseEntity.noContent().build();
    }
}

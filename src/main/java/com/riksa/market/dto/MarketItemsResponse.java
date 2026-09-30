package com.riksa.market.dto;

import java.util.List;

public record MarketItemsResponse(long balance, List<MarketItemResponse> items) {}

package com.riksa.buddy.dto;

import java.util.Map;

public record BuddyLookResponse(String species, String name, Map<String, String> equipped) {}

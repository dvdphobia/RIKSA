package com.riksa.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class SecurityStatsResponse {
    private long lockedAccounts;
    private long suspendedAccounts;
    private long totalFailedLoginAttempts;
}

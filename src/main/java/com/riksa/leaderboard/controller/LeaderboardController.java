package com.riksa.leaderboard.controller;

import com.riksa.common.constants.LeaderboardEndpoints;
import com.riksa.common.enums.ScopeType;
import com.riksa.leaderboard.dto.LeaderboardMapper;
import com.riksa.leaderboard.dto.LeaderboardRowResponse;
import com.riksa.leaderboard.dto.MyRankResponse;
import com.riksa.leaderboard.entity.LeaderboardSnapshot;
import com.riksa.leaderboard.service.LeaderboardQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping(LeaderboardEndpoints.BASE)
@RequiredArgsConstructor
public class LeaderboardController {

    private final LeaderboardQueryService leaderboardQueryService;

    @GetMapping(LeaderboardEndpoints.ROOT)
    public ResponseEntity<List<LeaderboardRowResponse>> getLeaderboard(
            @RequestParam(defaultValue = "COUNTRY") ScopeType scope,
            @RequestParam(defaultValue = "CAMBODIA") String scopeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        LocalDate queryDate = date != null ? date : LocalDate.now();
        var result = leaderboardQueryService.getLeaderboard(scope, scopeId, queryDate);

        return ResponseEntity.ok(result.stream()
                .map(LeaderboardMapper::toRow)
                .toList());
    }
    @GetMapping(LeaderboardEndpoints.ME)
    public ResponseEntity<MyRankResponse> getMyRank(
            @RequestParam(required = false) String scopeId) {

        UUID userId = getCurrentUserId();

        var countryRank = leaderboardQueryService.getMyRank(userId, ScopeType.COUNTRY, "CAMBODIA");
        Optional<LeaderboardSnapshot> provinceRank = scopeId != null
                ? leaderboardQueryService.getMyRank(userId, ScopeType.PROVINCE, scopeId)
                : Optional.empty();

        return ResponseEntity.ok(new MyRankResponse(
                countryRank.map(s -> s.getRank()).orElse(0),
                countryRank.map(s -> s.getScore()).orElse(0),
                countryRank.map(s -> s.getRankingTier()).orElse(null),
                provinceRank.map(s -> s.getRank()).orElse(null),
                provinceRank.map(s -> s.getScore()).orElse(null),
                provinceRank.map(s -> s.getRankingTier()).orElse(null)
        ));
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return UUID.fromString(auth.getName());
    }
}
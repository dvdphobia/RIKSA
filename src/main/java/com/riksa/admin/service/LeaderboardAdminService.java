package com.riksa.admin.service;

import com.riksa.admin.dto.LeaderboardBoardResponse;
import com.riksa.admin.dto.LeaderboardMetaResponse;
import com.riksa.admin.dto.LeaderboardRefreshResponse;
import com.riksa.auth.entity.Province;
import com.riksa.auth.entity.User;
import com.riksa.auth.repository.ProvinceRepository;
import com.riksa.auth.repository.UserRepository;
import com.riksa.common.audit.AuditLogService;
import com.riksa.common.enums.ScopeType;
import com.riksa.common.event.EventPublisher;
import com.riksa.leaderboard.entity.LeaderboardSnapshot;
import com.riksa.leaderboard.event.LeaderboardRefreshedEvent;
import com.riksa.leaderboard.repository.LeaderboardSnapshotRepository;
import com.riksa.leaderboard.service.LeaderboardQueryService;
import com.riksa.leaderboard.service.LeaderboardSnapshotService;
import com.riksa.leaderboard.service.RedisLeaderboardStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeaderboardAdminService {

    private static final int MAX_PAGE_SIZE = 100;

    private final LeaderboardSnapshotRepository snapshotRepository;
    private final LeaderboardQueryService leaderboardQueryService;
    private final LeaderboardSnapshotService snapshotService;
    private final RedisLeaderboardStore redisLeaderboardStore;
    private final UserRepository userRepository;
    private final ProvinceRepository provinceRepository;
    private final AuditLogService auditLogService;
    private final EventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public LeaderboardMetaResponse meta() {
        List<LocalDate> dates = snapshotRepository.findSnapshotDates();
        if (dates.isEmpty()) {
            return new LeaderboardMetaResponse(List.of(), List.of(
                    new LeaderboardMetaResponse.ScopeMeta(ScopeType.COUNTRY, "CAMBODIA", "Cambodia (national)", 0)));
        }

        LocalDate latest = dates.getFirst();
        List<LeaderboardSnapshot> latestRows = snapshotRepository.findBySnapshotDate(latest);
        Map<ScopeKey, Boolean> scopes = new LinkedHashMap<>();
        scopes.put(new ScopeKey(ScopeType.COUNTRY, "CAMBODIA"), true);
        latestRows.stream()
                .filter(row -> row.getScope() == ScopeType.PROVINCE)
                .map(row -> new ScopeKey(row.getScope(), row.getScopeId()))
                .forEach(key -> scopes.put(key, true));

        List<UUID> provinceIds = scopes.keySet().stream()
                .filter(key -> key.scope() == ScopeType.PROVINCE)
                .map(key -> UUID.fromString(key.scopeId()))
                .toList();
        Map<String, String> provinceNames = provinceRepository.findAllById(provinceIds).stream()
                .collect(Collectors.toMap(province -> province.getId().toString(), Province::getNameLatin));

        List<LeaderboardMetaResponse.ScopeMeta> scopeRows = new ArrayList<>();
        for (ScopeKey key : scopes.keySet()) {
            long userCount = leaderboardQueryService.getSnapshotLeaderboard(key.scope(), key.scopeId(), latest).size();
            String label = key.scope() == ScopeType.COUNTRY
                    ? "Cambodia (national)"
                    : provinceNames.getOrDefault(key.scopeId(), key.scopeId());
            scopeRows.add(new LeaderboardMetaResponse.ScopeMeta(key.scope(), key.scopeId(), label, userCount));
        }
        scopeRows.sort(Comparator.comparing((LeaderboardMetaResponse.ScopeMeta row) -> row.scope() == ScopeType.COUNTRY ? 0 : 1)
                .thenComparing(LeaderboardMetaResponse.ScopeMeta::label));
        return new LeaderboardMetaResponse(dates, scopeRows);
    }

    @Transactional(readOnly = true)
    public LeaderboardBoardResponse board(LocalDate requestedDate, ScopeType scope, String scopeId,
                                          int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        LocalDate date = requestedDate == null
                ? snapshotRepository.findSnapshotDates().stream()
                    .findFirst().orElse(LocalDate.now())
                : requestedDate;
        List<LeaderboardSnapshot> visible = leaderboardQueryService.getSnapshotLeaderboard(scope, scopeId, date);
        long offset = (long) safePage * safeSize;
        int from = (int) Math.min(offset, visible.size());
        int to = Math.min(from + safeSize, visible.size());
        List<LeaderboardSnapshot> selected = visible.subList(from, to);

        Map<UUID, User> usersById = userRepository.findAllById(
                        selected.stream().map(LeaderboardSnapshot::getUserId).toList())
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        List<LeaderboardBoardResponse.Row> rows = selected.stream()
                .map(snapshot -> toRow(snapshot, usersById.get(snapshot.getUserId())))
                .toList();
        return new LeaderboardBoardResponse(date, scope, scopeId, visible.size(), safePage, safeSize, rows);
    }

    public LeaderboardRefreshResponse refresh(UUID actorId) {
        LeaderboardSnapshotService.SnapshotCounts counts = snapshotService.refreshSnapshots();
        boolean redisRebuilt = false;
        if (redisLeaderboardStore.isEnabled()) {
            try {
                redisLeaderboardStore.replaceAll(snapshotService.loadSnapshots(LocalDate.now()));
                redisRebuilt = true;
            } catch (RuntimeException error) {
                log.error("PostgreSQL leaderboard refresh succeeded, but Redis rebuild failed", error);
            }
        }

        eventPublisher.publish(new LeaderboardRefreshedEvent(this, LocalDate.now(),
                counts.countryRows() + counts.provinceRows()));
        auditLogService.record(actorId, "LEADERBOARD_REFRESH", "LEADERBOARD", null,
                Map.of("countryRows", counts.countryRows(), "provinceRows", counts.provinceRows(),
                        "redisRebuilt", redisRebuilt));
        return new LeaderboardRefreshResponse(counts.countryRows(), counts.provinceRows(), redisRebuilt);
    }

    private LeaderboardBoardResponse.Row toRow(LeaderboardSnapshot snapshot, User user) {
        return new LeaderboardBoardResponse.Row(
                snapshot.getRank(),
                snapshot.getUserId(),
                user == null ? "Unknown user" : user.getUsername(),
                user == null ? null : user.getDisplayName(),
                snapshot.getScore(),
                snapshot.getRankingTier().name()
        );
    }

    private record ScopeKey(ScopeType scope, String scopeId) {}
}

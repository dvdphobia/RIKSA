package com.riksa.quest.controller;
import com.riksa.common.constants.QuestEndpoints;
import com.riksa.common.security.SecurityUtils;
import com.riksa.quest.dto.DailyQuestResponse;
import com.riksa.quest.dto.TodayQuestsResponse;
import com.riksa.quest.service.QuestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
@RestController
@RequestMapping(QuestEndpoints.BASE)
@RequiredArgsConstructor
public class QuestController {
    private final QuestService questService;
    @GetMapping(QuestEndpoints.TODAY)
    public ResponseEntity<TodayQuestsResponse> today() {
        return ResponseEntity.ok(questService.getToday(SecurityUtils.currentUserId()));
    }
    @PostMapping(QuestEndpoints.CLAIM)
    public ResponseEntity<DailyQuestResponse> claim(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(questService.claim(SecurityUtils.currentUserId(), id));
    }
}

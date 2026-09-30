package com.riksa.quest.setup;

import com.riksa.common.constants.QuestEndpoints;
import com.riksa.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** The one-time "get started" quests for new accounts. */
@RestController
@RequestMapping(QuestEndpoints.BASE + "/setup")
@RequiredArgsConstructor
public class SetupQuestController {

    private final SetupQuestService setupQuestService;

    @GetMapping
    public ResponseEntity<List<SetupTaskResponse>> list() {
        return ResponseEntity.ok(setupQuestService.list(SecurityUtils.currentUserId()));
    }

    @PostMapping("/{key}/claim")
    public ResponseEntity<SetupTaskResponse> claim(@PathVariable("key") String key) {
        return ResponseEntity.ok(setupQuestService.claim(SecurityUtils.currentUserId(), key));
    }
}

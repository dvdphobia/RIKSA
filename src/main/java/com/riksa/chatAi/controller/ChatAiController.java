package com.riksa.chatAi.controller;

import com.riksa.chatAi.dto.*;
import com.riksa.chatAi.service.ChatAiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai/chats")
@RequiredArgsConstructor
public class ChatAiController {
    private final ChatAiService service;
    @GetMapping public ResponseEntity<List<ChatSessionResponse>> list(Authentication auth) {
        return ResponseEntity.ok(service.list(userId(auth)));
    }
    @GetMapping("/{id}") public ResponseEntity<ChatSessionResponse> get(Authentication auth, @PathVariable UUID id) {
        return ResponseEntity.ok(service.get(userId(auth), id));
    }
    @PostMapping("/messages") public ResponseEntity<ChatTurnResponse> send(
            Authentication auth, @RequestBody @Valid SendChatMessageRequest request) {
        return ResponseEntity.ok(service.send(userId(auth), request));
    }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(Authentication auth, @PathVariable UUID id) {
        service.delete(userId(auth), id);
        return ResponseEntity.noContent().build();
    }
    private UUID userId(Authentication auth) { return UUID.fromString(auth.getName()); }
}

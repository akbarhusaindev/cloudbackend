package com.project.hussainproject.drive.controller;

import com.project.hussainproject.drive.dto.AiChatRequest;
import com.project.hussainproject.drive.dto.AiChatResponse;
import com.project.hussainproject.drive.model.User;
import com.project.hussainproject.drive.service.AiPdfService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * REST endpoints for AI-powered PDF analysis.
 *
 *  POST /api/files/{id}/ai-summary  → generate a summary of the PDF
 *  POST /api/files/{id}/ai-chat     → answer a question about the PDF
 */
@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class AiPdfController {

    private final AiPdfService aiPdfService;

    /**
     * Generate a bullet-point summary of a PDF file.
     *
     * POST /api/files/{id}/ai-summary
     * Response: { "result": "• Point 1\n• Point 2\n..." }
     */
    @PostMapping("/{id}/ai-summary")
    public ResponseEntity<?> summarise(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user,
            HttpServletRequest request) {

        try {
            String summary = aiPdfService.summarise(id, user);
            return ResponseEntity.ok(new AiChatResponse(summary));
        } catch (IllegalArgumentException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage(), request);
        } catch (SecurityException e) {
            return error(HttpStatus.FORBIDDEN, e.getMessage(), request);
        } catch (Exception e) {
            log.error("AI summary failed for file {}: {}", id, e.getMessage());
            return error(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), request);
        }
    }

    /**
     * Answer a user question about a PDF file.
     *
     * POST /api/files/{id}/ai-chat
     * Body: { "question": "What is the main conclusion?" }
     * Response: { "result": "The main conclusion is..." }
     */
    @PostMapping("/{id}/ai-chat")
    public ResponseEntity<?> chat(
            @PathVariable UUID id,
            @RequestBody AiChatRequest chatRequest,
            @AuthenticationPrincipal User user,
            HttpServletRequest request) {

        if (chatRequest.getQuestion() == null || chatRequest.getQuestion().isBlank()) {
            return error(HttpStatus.BAD_REQUEST, "Question must not be empty", request);
        }

        try {
            String answer = aiPdfService.chat(id, user, chatRequest.getQuestion());
            return ResponseEntity.ok(new AiChatResponse(answer));
        } catch (IllegalArgumentException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage(), request);
        } catch (SecurityException e) {
            return error(HttpStatus.FORBIDDEN, e.getMessage(), request);
        } catch (Exception e) {
            log.error("AI chat failed for file {}: {}", id, e.getMessage());
            return error(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), request);
        }
    }

    // ── Helper ────────────────────────────────────────────────────────
    private ResponseEntity<Map<String, Object>> error(
            HttpStatus status, String message, HttpServletRequest req) {
        return ResponseEntity.status(status).body(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message != null ? message : "Unknown error",
                "path", req.getRequestURI()
        ));
    }
}


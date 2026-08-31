package com.project.hussainproject.drive.controller;


import com.project.hussainproject.drive.dto.PublicLinkRequest;
import com.project.hussainproject.drive.model.PublicLink;
import com.project.hussainproject.drive.model.User;
import com.project.hussainproject.drive.repository.PublicLinkRepository;
import com.project.hussainproject.drive.service.PublicLinkService;
import com.project.hussainproject.drive.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/public-links")
@RequiredArgsConstructor
public class PublicLinkController {

    private final PublicLinkService publicLinkService;
    private final PublicLinkRepository publicLinkRepository;
    private final StorageService storageService;

    /**
     * POST /api/public-links
     * Creates a new public shareable link for a file.
     */
    @PostMapping
    public ResponseEntity<PublicLink> createLink(
            @RequestBody PublicLinkRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(publicLinkService.createLink(request, user));
    }

    /**
     * GET /api/public-links/{token}
     * Resolves a public link token and returns file info + presigned download URL.
     * No authentication required.
     */
    @GetMapping("/{token}")
    public ResponseEntity<Map<String, Object>> resolveLink(@PathVariable String token) {
        PublicLink link = publicLinkRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Link not found or expired"));

        // Check expiry
        if (link.getExpiresAt() != null && link.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
            return ResponseEntity.status(410).build(); // Gone
        }

        String downloadUrl = storageService.generateDownloadUrl(link.getFile().getStorageKey());

        return ResponseEntity.ok(Map.of(
                "id", link.getFile().getId(),
                "originalName", link.getFile().getOriginalName(),
                "mimeType", link.getFile().getMimeType(),
                "size", link.getFile().getSize(),
                "downloadUrl", downloadUrl,
                "token", token
        ));
    }
}

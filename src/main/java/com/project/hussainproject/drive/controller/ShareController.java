package com.project.hussainproject.drive.controller;

import com.project.hussainproject.drive.dto.ShareRequest;
import com.project.hussainproject.drive.dto.SharedFolderResponse;
import com.project.hussainproject.drive.model.Share;
import com.project.hussainproject.drive.model.User;
import com.project.hussainproject.drive.service.ShareService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shares")
@RequiredArgsConstructor
public class ShareController {

    private final ShareService shareService;

    /**
     * Share a file with another user.
     * Automatically creates (or reuses) a sender-named folder in the
     * recipient's root and links the file to it.
     *
     * POST /api/shares
     * Body: { "fileId": "<uuid>", "targetEmail": "bob@example.com", "permission": "VIEWER" }
     */
    @PostMapping
    public ResponseEntity<Share> shareFile(
            @RequestBody ShareRequest request,
            @AuthenticationPrincipal User user) {

        return ResponseEntity.ok(shareService.shareFile(request, user));
    }

    /**
     * Classic flat list — all Share records where the caller is the recipient.
     *
     * GET /api/shares/me
     */
    @GetMapping("/me")
    public ResponseEntity<List<Share>> getSharedWithMe(
            @AuthenticationPrincipal User user) {

        return ResponseEntity.ok(shareService.getSharedWithMe(user));
    }

    /**
     * Grouped view — returns sender-named folders with the files inside each.
     * Use this endpoint to render the "Shared with me" section of the UI.
     *
     * GET /api/shares/me/folders
     *
     * Example response:
     * [
     *   {
     *     "folderId": "...",
     *     "folderName": "alice@gmail.com",
     *     "createdAt": "2026-10-29T10:00:00",
     *     "files": [ { ...file fields... } ]
     *   }
     * ]
     */
    @GetMapping("/me/folders")
    public ResponseEntity<List<SharedFolderResponse>> getSharedFolders(
            @AuthenticationPrincipal User user) {

        return ResponseEntity.ok(shareService.getSharedFolders(user));
    }
}

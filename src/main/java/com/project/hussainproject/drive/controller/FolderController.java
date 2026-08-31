package com.project.hussainproject.drive.controller;

import com.project.hussainproject.drive.dto.CreateFolderRequest;
import com.project.hussainproject.drive.model.Folder;
import com.project.hussainproject.drive.model.User;
import com.project.hussainproject.drive.repository.FolderRepository;
import com.project.hussainproject.drive.service.FolderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/folders")
@RequiredArgsConstructor
public class FolderController {

    private final FolderService folderService;
    private final FolderRepository folderRepository;

    /**
     * GET /api/folders
     * Returns all root folders (no parent) for the current user.
     */
    @GetMapping
    public ResponseEntity<List<Folder>> getRootFolders(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(folderRepository.findByOwnerAndParentFolderIsNullAndIsTrashedFalse(user));
    }

    /**
     * GET /api/folders/{id}/children
     * Returns direct child folders inside a given folder.
     */
    @GetMapping("/{id}/children")
    public ResponseEntity<List<Folder>> getChildFolders(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(folderRepository.findByOwnerAndParentFolderIdAndIsTrashedFalse(user, id));
    }

    /**
     * POST /api/folders
     * Creates a new folder (optionally inside a parent folder).
     */
    @PostMapping
    public ResponseEntity<Folder> createFolder(
            @RequestBody CreateFolderRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(folderService.createFolder(request, user));
    }

    /**
     * PUT /api/folders/{id}/rename
     */
    @PutMapping("/{id}/rename")
    public ResponseEntity<Folder> renameFolder(
            @PathVariable UUID id,
            @RequestParam String newName,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(folderService.renameFolder(id, newName, user));
    }
}

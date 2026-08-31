package com.project.hussainproject.drive.controller;

import com.project.hussainproject.drive.model.FileMetadata;
import com.project.hussainproject.drive.model.Folder;
import com.project.hussainproject.drive.model.User;
import com.project.hussainproject.drive.repository.FileRepository;
import com.project.hussainproject.drive.repository.FolderRepository;
import com.project.hussainproject.drive.service.FileService;
import com.project.hussainproject.drive.service.FolderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/trash")
@RequiredArgsConstructor
public class TrashController {

    private final FileService fileService;
    private final FolderService folderService;
    private final FileRepository fileRepository;
    private final FolderRepository folderRepository;

    /**
     * GET /api/trash/files
     * Returns all trashed files for the current user.
     */
    @GetMapping("/files")
    public ResponseEntity<List<FileMetadata>> getTrashedFiles(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(fileRepository.findByOwnerAndIsTrashedTrue(user));
    }

    /**
     * GET /api/trash/folders
     * Returns all trashed folders for the current user.
     */
    @GetMapping("/folders")
    public ResponseEntity<List<Folder>> getTrashedFolders(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(folderRepository.findByOwnerAndIsTrashedTrue(user));
    }

    /**
     * POST /api/trash/files/{id}
     * Moves a file to trash (soft delete).
     */
    @PostMapping("/files/{id}")
    public ResponseEntity<Void> trashFile(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        fileService.trashFile(id, user);
        return ResponseEntity.ok().build();
    }

    /**
     * POST /api/trash/folders/{id}
     * Moves a folder to trash (soft delete recursive).
     */
    @PostMapping("/folders/{id}")
    public ResponseEntity<Void> trashFolder(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        folderService.trashFolder(id, user);
        return ResponseEntity.ok().build();
    }

    /**
     * POST /api/trash/files/{id}/restore
     * Restores a file from trash.
     */
    @PostMapping("/files/{id}/restore")
    public ResponseEntity<FileMetadata> restoreFile(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(fileService.restoreFile(id, user));
    }

    /**
     * POST /api/trash/folders/{id}/restore
     * Restores a folder from trash.
     */
    @PostMapping("/folders/{id}/restore")
    public ResponseEntity<Void> restoreFolder(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        folderService.restoreFolder(id, user);
        return ResponseEntity.ok().build();
    }

    /**
     * DELETE /api/trash/files/{id}
     * Permanently deletes a file from trash.
     */
    @DeleteMapping("/files/{id}")
    public ResponseEntity<Void> permanentlyDeleteFile(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        fileService.permanentlyDeleteFile(id, user);
        return ResponseEntity.noContent().build();
    }

    /**
     * DELETE /api/trash/folders/{id}
     * Permanently deletes a folder from trash.
     */
    @DeleteMapping("/folders/{id}")
    public ResponseEntity<Void> permanentlyDeleteFolder(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        folderService.permanentlyDeleteFolder(id, user);
        return ResponseEntity.noContent().build();
    }
}

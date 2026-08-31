package com.project.hussainproject.drive.controller;

import com.project.hussainproject.drive.model.FileMetadata;
import com.project.hussainproject.drive.model.User;
import com.project.hussainproject.drive.repository.FileRepository;
import com.project.hussainproject.drive.repository.ShareRepository;
import com.project.hussainproject.drive.service.FileService;
import com.project.hussainproject.drive.service.StorageService;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;


@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final StorageService storageService;
    private final FileRepository fileRepository;
    private final ShareRepository shareRepository;
    private final FileService fileService;

    /**
     * GET /api/files
     * Returns all non-trashed files in the root (no folder) for the current user.
     */
    @GetMapping
    public ResponseEntity<List<FileMetadata>> getRootFiles(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(fileRepository.findByOwnerAndFolderIsNullAndIsTrashedFalse(user));
    }

    /**
     * GET /api/files/folder/{folderId}
     * Returns all non-trashed files inside a specific folder.
     */
    @GetMapping("/folder/{folderId}")
    public ResponseEntity<List<FileMetadata>> getFilesInFolder(
            @PathVariable UUID folderId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(fileRepository.findByOwnerAndFolderIdAndIsTrashedFalse(user, folderId));
    }

    /**
     * POST /api/files/init-upload
     * Generates a presigned S3 PUT URL for direct browser upload.
     */
    @PostMapping("/init-upload")
    public ResponseEntity<Map<String, String>> initUpload(
            @RequestParam String fileName,
            @RequestParam String mimeType) {
        String storageKey = UUID.randomUUID().toString() + "-" + fileName;
        String uploadUrl = storageService.generateUploadUrl(storageKey, mimeType);

        return ResponseEntity.ok(Map.of(
                "uploadUrl", uploadUrl,
                "storageKey", storageKey
        ));
    }

    /**
     * POST /api/files/complete-upload
     * Saves file metadata to DB after the browser has uploaded to S3.
     */
    @PostMapping("/complete-upload")
    public ResponseEntity<FileMetadata> completeUpload(
            @AuthenticationPrincipal User user,
            @RequestParam String fileName,
            @RequestParam String storageKey,
            @RequestParam Long size,
            @RequestParam String mimeType,
            @RequestParam(required = false) UUID folderId) {

        FileMetadata fileMetadata = FileMetadata.builder()
                .originalName(fileName)
                .storageKey(storageKey)
                .size(size)
                .mimeType(mimeType)
                .owner(user)
                .isTrashed(false)
                .build();

        // If a folderId is provided, attach to folder
        if (folderId != null) {
            fileMetadata.setFolder(
                    com.project.hussainproject.drive.model.Folder.builder().id(folderId).build()
            );
        }

        FileMetadata savedFile = fileRepository.save(fileMetadata);
        return ResponseEntity.ok(savedFile);
    }

    /**
     * GET /api/files/{id}/download-url
     * Returns a presigned GET URL so the browser can download the file directly from S3.
     */
    @GetMapping("/{id}/download-url")
    public ResponseEntity<Map<String, String>> getDownloadUrl(
            @PathVariable UUID id,
            @RequestParam(required = false, defaultValue = "inline") String disposition,
            @AuthenticationPrincipal User user) {
        FileMetadata file = fileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File not found"));

        boolean isOwner = file.getOwner().getId().equals(user.getId());
        boolean isShared = shareRepository.findByFileIdAndSharedWithId(id, user.getId()).isPresent();

        if (!isOwner && !isShared) {
            throw new RuntimeException("Access denied: You do not have permission to view this file.");
        }

        String url = storageService.generateDownloadUrl(file.getStorageKey(), disposition);
        return ResponseEntity.ok(Map.of("downloadUrl", url, "fileName", file.getOriginalName()));
    }

    /**
     * GET /api/files/storage
     */
    @GetMapping("/storage")
    public ResponseEntity<Map<String, Object>> getStorageUsage(@AuthenticationPrincipal User user) {
        Long used = fileRepository.sumSizeByOwnerAndIsTrashedFalse(user);
        long total = 15L * 1024 * 1024 * 1024; // 15 GB
        return ResponseEntity.ok(Map.of("used", used, "total", total));
    }

    /**
     * PUT /api/files/{id}/rename
     */
    @PutMapping("/{id}/rename")
    public ResponseEntity<FileMetadata> renameFile(
            @PathVariable UUID id,
            @RequestParam String newName,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(fileService.renameFile(id, newName, user));
    }

    /**
     * PUT /api/files/{id}/move
     */
    @PutMapping("/{id}/move")
    public ResponseEntity<FileMetadata> moveFile(
            @PathVariable UUID id,
            @RequestParam(required = false) UUID folderId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(fileService.moveFile(id, folderId, user));
    }
}

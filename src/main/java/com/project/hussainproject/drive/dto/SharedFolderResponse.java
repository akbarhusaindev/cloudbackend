package com.project.hussainproject.drive.dto;

import com.project.hussainproject.drive.model.FileMetadata;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Represents a sender-named folder together with the files inside it.
 * Returned by GET /api/shares/me/folders.
 *
 * Example payload:
 * {
 *   "folderId": "...",
 *   "folderName": "alice@gmail.com",
 *   "createdAt": "2026-10-29T10:00:00",
 *   "files": [ { ...FileMetadata fields... } ]
 * }
 */
@Value
@Builder
public class SharedFolderResponse {

    /** ID of the auto-created sender-named folder. */
    UUID folderId;

    /** The sender's e-mail address (folder name). */
    String folderName;

    /** When the folder was first created (= first share from this sender). */
    LocalDateTime createdAt;

    /** All files shared into this folder by the sender. */
    List<FileMetadata> files;
}


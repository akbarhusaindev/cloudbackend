package com.project.hussainproject.drive.service;

import com.project.hussainproject.drive.model.FileMetadata;
import com.project.hussainproject.drive.model.Folder;
import com.project.hussainproject.drive.model.SharePermission;
import com.project.hussainproject.drive.model.User;
import com.project.hussainproject.drive.repository.FileRepository;
import com.project.hussainproject.drive.repository.FolderRepository;
import com.project.hussainproject.drive.repository.PublicLinkRepository;
import com.project.hussainproject.drive.repository.ShareRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {

    private final FileRepository fileRepository;
    private final FolderRepository folderRepository;
    private final ShareRepository shareRepository;
    private final PublicLinkRepository publicLinkRepository;

    public FileMetadata renameFile(UUID fileId, String newName, User user) {
        FileMetadata file = getFileWithWriteAccess(fileId, user);
        file.setOriginalName(newName);
        return fileRepository.save(file);
    }

    public FileMetadata moveFile(UUID fileId, UUID targetFolderId, User user) {
        FileMetadata file = getFileWithWriteAccess(fileId, user);
        Folder targetFolder = null;

        if (targetFolderId != null) {
            targetFolder = folderRepository.findById(targetFolderId)
                    .filter(f -> f.getOwner().getId().equals(user.getId()))
                    .orElseThrow(() -> new RuntimeException("Target folder not found"));
        }

        file.setFolder(targetFolder);
        return fileRepository.save(file);
    }

    public void trashFile(UUID fileId, User user) {
        FileMetadata file = getFileWithWriteAccess(fileId, user);
        file.setTrashed(true);
        fileRepository.save(file);
    }

    /**
     * Restores a file from trash back to its original location.
     */
    public FileMetadata restoreFile(UUID fileId, User user) {
        FileMetadata file = fileRepository.findById(fileId)
                .filter(f -> f.getOwner().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("File not found or access denied"));
        file.setTrashed(false);
        return fileRepository.save(file);
    }

    /**
     * Permanently deletes a file record from the database.
     * Deletes related share and public link records first to avoid foreign key errors.
     */
    @Transactional
    public void permanentlyDeleteFile(UUID fileId, User user) {
        FileMetadata file = fileRepository.findById(fileId)
                .filter(f -> f.getOwner().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("File not found or access denied"));

        // Delete all shares associated with this file
        shareRepository.deleteByFileId(fileId);

        // Delete all public link shares associated with this file
        publicLinkRepository.deleteByFileId(fileId);

        // Finally delete file metadata
        fileRepository.delete(file);
    }

    /**
     * Retrieves file if user is the owner OR user has EDITOR access shared with them.
     */
    private FileMetadata getFileWithWriteAccess(UUID fileId, User user) {
        FileMetadata file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));

        if (file.getOwner().getId().equals(user.getId())) {
            return file;
        }

        boolean hasEditorAccess = shareRepository.findByFileIdAndSharedWithId(fileId, user.getId())
                .filter(share -> share.getPermission() == SharePermission.EDITOR)
                .isPresent();

        if (hasEditorAccess) {
            return file;
        }

        throw new RuntimeException("Access denied: You do not have EDITOR permission on this file.");
    }
}

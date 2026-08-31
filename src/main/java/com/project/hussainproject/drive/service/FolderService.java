package com.project.hussainproject.drive.service;

import com.project.hussainproject.drive.dto.CreateFolderRequest;
import com.project.hussainproject.drive.model.FileMetadata;
import com.project.hussainproject.drive.model.Folder;
import com.project.hussainproject.drive.model.User;
import com.project.hussainproject.drive.repository.FileRepository;
import com.project.hussainproject.drive.repository.FolderRepository;
import com.project.hussainproject.drive.repository.PublicLinkRepository;
import com.project.hussainproject.drive.repository.ShareRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FolderService {
    private final FolderRepository folderRepository;
    private final FileRepository fileRepository;
    private final ShareRepository shareRepository;
    private final PublicLinkRepository publicLinkRepository;

    public Folder createFolder(CreateFolderRequest request, User user) {
        Folder parent = null;

        if (request.getParentId() != null && !request.getParentId().isEmpty()) {
            parent = folderRepository.findById(UUID.fromString(request.getParentId()))
                    .filter(f -> f.getOwner().getId().equals(user.getId()))
                    .orElseThrow(() -> new RuntimeException("Parent folder not found or access denied"));
        }

        Folder folder = Folder.builder()
                .name(request.getName())
                .parentFolder(parent)
                .owner(user)
                .isTrashed(false)
                .build();

        return folderRepository.save(folder);
    }

    public Folder renameFolder(UUID folderId, String newName, User user) {
        Folder folder = folderRepository.findById(folderId)
                .filter(f -> f.getOwner().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Folder not found or access denied"));
        folder.setName(newName);
        return folderRepository.save(folder);
    }

    /**
     * Move folder to trash recursively (soft-delete).
     */
    @Transactional
    public void trashFolder(UUID folderId, User user) {
        Folder folder = folderRepository.findById(folderId)
                .filter(f -> f.getOwner().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Folder not found or access denied"));
        trashFolderRecursive(folder);
    }

    private void trashFolderRecursive(Folder folder) {
        folder.setTrashed(true);
        folderRepository.save(folder);

        // Soft delete all active child files
        List<FileMetadata> files = fileRepository.findByOwnerAndFolderIdAndIsTrashedFalse(folder.getOwner(), folder.getId());
        for (FileMetadata file : files) {
            file.setTrashed(true);
            fileRepository.save(file);
        }

        // Recursively soft delete active child folders
        List<Folder> subfolders = folderRepository.findByOwnerAndParentFolderIdAndIsTrashedFalse(folder.getOwner(), folder.getId());
        for (Folder sub : subfolders) {
            trashFolderRecursive(sub);
        }
    }

    /**
     * Restore folder from trash recursively.
     */
    @Transactional
    public void restoreFolder(UUID folderId, User user) {
        Folder folder = folderRepository.findById(folderId)
                .filter(f -> f.getOwner().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Folder not found or access denied"));
        restoreFolderRecursive(folder);
    }

    private void restoreFolderRecursive(Folder folder) {
        folder.setTrashed(false);
        folderRepository.save(folder);

        // Restore child files that were trashed
        List<FileMetadata> files = fileRepository.findByOwnerAndFolderIdAndIsTrashedTrue(folder.getOwner(), folder.getId());
        for (FileMetadata file : files) {
            file.setTrashed(false);
            fileRepository.save(file);
        }

        // Recursively restore child folders
        List<Folder> subfolders = folderRepository.findByOwnerAndParentFolderIdAndIsTrashedTrue(folder.getOwner(), folder.getId());
        for (Folder sub : subfolders) {
            restoreFolderRecursive(sub);
        }
    }

    /**
     * Permanently delete folder recursively.
     */
    @Transactional
    public void permanentlyDeleteFolder(UUID folderId, User user) {
        Folder folder = folderRepository.findById(folderId)
                .filter(f -> f.getOwner().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Folder not found or access denied"));
        deleteFolderRecursive(folder);
    }

    private void deleteFolderRecursive(Folder folder) {
        // Delete all child files first
        List<FileMetadata> files = fileRepository.findByOwnerAndFolderIdAndIsTrashedTrue(folder.getOwner(), folder.getId());
        for (FileMetadata file : files) {
            // Delete related shares and public links to avoid constraint violations
            shareRepository.deleteByFileId(file.getId());
            publicLinkRepository.deleteByFileId(file.getId());
        }
        fileRepository.deleteAll(files);

        // Recursively delete child folders
        List<Folder> subfolders = folderRepository.findByOwnerAndParentFolderIdAndIsTrashedTrue(folder.getOwner(), folder.getId());
        for (Folder sub : subfolders) {
            deleteFolderRecursive(sub);
        }

        folderRepository.delete(folder);
    }
}

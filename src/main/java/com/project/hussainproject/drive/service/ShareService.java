package com.project.hussainproject.drive.service;

import com.project.hussainproject.drive.dto.ShareRequest;
import com.project.hussainproject.drive.dto.SharedFolderResponse;
import com.project.hussainproject.drive.exception.ResourceNotFoundException;
import com.project.hussainproject.drive.model.*;
import com.project.hussainproject.drive.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ShareService {

    private final ShareRepository shareRepository;
    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final FolderRepository folderRepository;
    private final SharedFolderFileRepository sharedFolderFileRepository;

    /**
     * Share a file with another user.
     *
     * Smart Folder Logic (runs inside the same transaction):
     * 1. Find or create a root-level folder in the RECIPIENT's account
     *    whose name equals the SENDER's e-mail address.
     * 2. Create a {@link SharedFolderFile} record linking the file to
     *    that folder — this makes the file appear in the recipient's
     *    "Shared with me" folder view.
     * 3. Create/update the classic {@link Share} record (permission-aware).
     */
    @Transactional
    public Share shareFile(ShareRequest request, User owner) {

        // ── 1. Load & validate file ────────────────────────────────────────
        FileMetadata file = fileRepository.findById(request.getFileId())
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));

        if (!file.getOwner().getId().equals(owner.getId())) {
            throw new IllegalArgumentException("You are not allowed to share this file");
        }

        // ── 2. Load recipient ──────────────────────────────────────────────
        User targetUser = userRepository.findByEmail(request.getTargetEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Target user not found"));

        if (targetUser.getId().equals(owner.getId())) {
            throw new IllegalArgumentException("You cannot share a file with yourself");
        }

        // ── 3. Smart folder: find or create "sender@email.com" folder ──────
        Folder senderFolder = folderRepository
                .findByOwnerAndNameAndParentFolderIsNull(targetUser, owner.getEmail())
                .orElseGet(() -> folderRepository.save(
                        Folder.builder()
                                .name(owner.getEmail())   // folder named after sender's e-mail
                                .owner(targetUser)         // belongs to the recipient
                                .parentFolder(null)        // lives at root
                                .isTrashed(false)
                                .build()
                ));

        // ── 4. Link file → sender folder (idempotent) ─────────────────────
        sharedFolderFileRepository
                .findByFolderIdAndFileId(senderFolder.getId(), file.getId())
                .orElseGet(() -> sharedFolderFileRepository.save(
                        SharedFolderFile.builder()
                                .folder(senderFolder)
                                .file(file)
                                .sharedBy(owner)
                                .build()
                ));

        // ── 5. Classic share record (with permission) ──────────────────────
        Share share = shareRepository
                .findByFileIdAndSharedWithId(file.getId(), targetUser.getId())
                .orElse(Share.builder()
                        .file(file)
                        .sharedWith(targetUser)
                        .sharedBy(owner)
                        .build());

        share.setPermission(request.getPermission());
        return shareRepository.save(share);
    }

    // ── Read helpers ───────────────────────────────────────────────────────

    /** Classic: all Share records where this user is the recipient. */
    public List<Share> getSharedWithMe(User user) {
        return shareRepository.findBySharedWith(user);
    }

    /**
     * Returns a list of "sender folders" visible to the recipient,
     * each paired with the files inside — ready for a grouped UI view.
     */
    public List<SharedFolderResponse> getSharedFolders(User recipient) {
        // Get all root folders of the recipient that were auto-created by sharing
        List<Folder> senderFolders = folderRepository
                .findByOwnerAndParentFolderIsNullAndIsTrashedFalse(recipient)
                .stream()
                // Only include folders whose name looks like an e-mail (sender-named)
                .filter(f -> f.getName().contains("@"))
                .toList();

        return senderFolders.stream().map(folder -> {
            List<SharedFolderFile> items =
                    sharedFolderFileRepository.findByFolderId(folder.getId());
            return SharedFolderResponse.builder()
                    .folderId(folder.getId())
                    .folderName(folder.getName())   // sender's e-mail
                    .createdAt(folder.getCreatedAt())
                    .files(items.stream().map(SharedFolderFile::getFile).toList())
                    .build();
        }).toList();
    }
}

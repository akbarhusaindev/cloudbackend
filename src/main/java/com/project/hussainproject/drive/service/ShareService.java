package com.project.hussainproject.drive.service;

import com.project.hussainproject.drive.dto.ShareRequest;
import com.project.hussainproject.drive.exception.ResourceNotFoundException;
import com.project.hussainproject.drive.model.FileMetadata;
import com.project.hussainproject.drive.model.Share;
import com.project.hussainproject.drive.model.User;
import com.project.hussainproject.drive.repository.FileRepository;
import com.project.hussainproject.drive.repository.ShareRepository;
import com.project.hussainproject.drive.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ShareService {

    private final ShareRepository shareRepository;
    private final FileRepository fileRepository;
    private final UserRepository userRepository;

    public Share shareFile(ShareRequest request, User owner) {

        // 1. Find file
        FileMetadata file = fileRepository.findById(request.getFileId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("File not found")
                );

        // 2. Check ownership
        if (!file.getOwner().getId().equals(owner.getId())) {
            throw new RuntimeException(
                    "You are not allowed to share this file"
            );
        }

        // 3. Find target user
        User targetUser = userRepository.findByEmail(request.getTargetEmail())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Target user not found")
                );

        // 4. Prevent sharing with yourself
        if (targetUser.getId().equals(owner.getId())) {
            throw new IllegalArgumentException(
                    "You cannot share a file with yourself"
            );
        }

        // 5. Find existing share or create new one
        Share share = shareRepository
                .findByFileIdAndSharedWithId(
                        file.getId(),
                        targetUser.getId()
                )
                .orElse(
                        Share.builder()
                                .file(file)
                                .sharedWith(targetUser)
                                .sharedBy(owner)
                                .build()
                );

        // 6. Set permission
        share.setPermission(request.getPermission());

        // 7. Save
        return shareRepository.save(share);
    }

    public List<Share> getSharedWithMe(User user) {
        return shareRepository.findBySharedWith(user);
    }
}
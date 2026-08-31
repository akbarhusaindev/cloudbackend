package com.project.hussainproject.drive.service;


import com.project.hussainproject.drive.dto.PublicLinkRequest;
import com.project.hussainproject.drive.model.FileMetadata;
import com.project.hussainproject.drive.model.PublicLink;
import com.project.hussainproject.drive.model.User;
import com.project.hussainproject.drive.repository.FileRepository;
import com.project.hussainproject.drive.repository.PublicLinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PublicLinkService {

    private final PublicLinkRepository publicLinkRepository;
    private final FileRepository fileRepository;
    private final PasswordEncoder passwordEncoder;

    public PublicLink createLink(PublicLinkRequest request, User owner) {
        FileMetadata file = fileRepository.findById(request.getFileId())
                .filter(f->f.getOwner().getId().equals(owner.getId()))
                .orElseThrow(()-> new RuntimeException("file not found or access denied"));

        LocalDateTime expiry = request.getExpiryDays() !=null ? LocalDateTime.now().plusDays(request.getExpiryDays()) :null;

        String encodedPassword = request.getPassword()!=null && !request.getPassword().isEmpty() ? passwordEncoder.encode(request.getPassword()) : null;

        PublicLink link = PublicLink.builder()
                .file(file)
                .token(UUID.randomUUID().toString())
                .permission(request.getPermission())
                .expiresAt(expiry)
                .password(encodedPassword)
                .build();

        return publicLinkRepository.save(link);
    }
}

package com.project.hussainproject.drive.repository;

import com.project.hussainproject.drive.model.PublicLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PublicLinkRepository extends JpaRepository<PublicLink, UUID> {

    Optional<PublicLink> findByToken(String token);

    void deleteByFileId(UUID fileId);
}

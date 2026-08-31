package com.project.hussainproject.drive.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.hussainproject.drive.model.Share;
import com.project.hussainproject.drive.model.User;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShareRepository extends JpaRepository<Share, UUID> {
    List<Share> findBySharedWith(User user);

    Optional<Share> findByFileIdAndSharedWithId(UUID fileId, UUID userId);

    void deleteByFileId(UUID fileId);
}

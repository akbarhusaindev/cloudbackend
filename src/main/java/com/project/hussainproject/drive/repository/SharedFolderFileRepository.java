package com.project.hussainproject.drive.repository;

import com.project.hussainproject.drive.model.SharedFolderFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SharedFolderFileRepository extends JpaRepository<SharedFolderFile, UUID> {

    /** Prevent duplicate sharing records for the same file–folder pair. */
    Optional<SharedFolderFile> findByFolderIdAndFileId(UUID folderId, UUID fileId);

    /** Fetch all files that appear inside a given sender-named folder. */
    List<SharedFolderFile> findByFolderId(UUID folderId);

    /** Fetch all sharing records where a specific file was shared. */
    List<SharedFolderFile> findByFileId(UUID fileId);

    /** Remove all sharing records when a file is permanently deleted. */
    void deleteByFileId(UUID fileId);
}


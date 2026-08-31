package com.project.hussainproject.drive.repository;

import com.project.hussainproject.drive.model.FileMetadata;
import com.project.hussainproject.drive.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FileRepository extends JpaRepository<FileMetadata, UUID> {
    List<FileMetadata> findByOwnerAndFolderIsNullAndIsTrashedFalse(User owner);
    List<FileMetadata> findByOwnerAndFolderIdAndIsTrashedFalse(User owner, UUID folderId);
    List<FileMetadata> findByOwnerAndIsTrashedTrue(User owner);
    List<FileMetadata> findByOwnerAndFolderIdAndIsTrashedTrue(User owner, UUID folderId);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(f.size), 0) FROM FileMetadata f WHERE f.owner = :owner AND f.isTrashed = false")
    Long sumSizeByOwnerAndIsTrashedFalse(@org.springframework.data.repository.query.Param("owner") User owner);

    Page<FileMetadata> findByOwnerAndOriginalNameContainingIgnoreCaseAndIsTrashedFalse(User owner, String keyword, Pageable pageable);
}

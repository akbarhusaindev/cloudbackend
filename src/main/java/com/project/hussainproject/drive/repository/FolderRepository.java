package com.project.hussainproject.drive.repository;

import com.project.hussainproject.drive.model.Folder;
import com.project.hussainproject.drive.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FolderRepository extends JpaRepository<Folder, UUID> {

    List<Folder> findByOwnerAndParentFolderIsNullAndIsTrashedFalse(User owner);

    List<Folder> findByOwnerAndParentFolderIdAndIsTrashedFalse(
            User owner,
            UUID parentId
    );

    List<Folder> findByOwnerAndIsTrashedTrue(User owner);

    List<Folder> findByOwnerAndParentFolderIdAndIsTrashedTrue(User owner, UUID parentId);

    Page<Folder> findByOwnerAndNameContainingIgnoreCaseAndIsTrashedFalse(User owner,String keyword ,Pageable pageable);
}
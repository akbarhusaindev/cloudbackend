package com.project.hussainproject.drive.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Links a shared FileMetadata to a virtual "sender-named" folder
 * that lives in the recipient's root directory.
 *
 * Example: When alice@gmail.com shares a file with bob@gmail.com,
 * a Folder named "alice@gmail.com" is created in bob's root (parentFolder = null),
 * and a SharedFolderFile record ties the file to that folder.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "shared_folder_files",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_shared_folder_file",
        columnNames = {"folder_id", "file_id"}
    ),
    indexes = {
        @Index(name = "idx_sff_folder_id", columnList = "folder_id"),
        @Index(name = "idx_sff_file_id",   columnList = "file_id")
    }
)
public class SharedFolderFile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** The sender-named folder that lives in the *recipient's* root. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "owner", "parentFolder"})
    private Folder folder;

    /** The original file being shared. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "file_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "owner", "folder"})
    private FileMetadata file;

    /** The user who initiated the share (the sender). */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "shared_by_user_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "password", "role",
            "authorities", "accountNonExpired", "accountNonLocked",
            "credentialsNonExpired", "enabled"})
    private User sharedBy;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}


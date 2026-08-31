package com.project.hussainproject.drive.service;


import com.project.hussainproject.drive.dto.SearchResponse;
import com.project.hussainproject.drive.model.FileMetadata;
import com.project.hussainproject.drive.model.Folder;
import com.project.hussainproject.drive.model.User;
import com.project.hussainproject.drive.repository.FileRepository;
import com.project.hussainproject.drive.repository.FolderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final FolderRepository folderRepository;
    private final FileRepository fileRepository;

    public SearchResponse search(String keyword, int page, int size, User user) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Folder> folders = folderRepository
                .findByOwnerAndNameContainingIgnoreCaseAndIsTrashedFalse(user, keyword, pageable);

        Page<FileMetadata> files = fileRepository
                .findByOwnerAndOriginalNameContainingIgnoreCaseAndIsTrashedFalse(user, keyword, pageable);


        return SearchResponse.builder()
                .folders(folders)
                .files(files)
                .query(keyword)
                .build();




    }


}

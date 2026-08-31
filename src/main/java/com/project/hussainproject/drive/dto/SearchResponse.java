package com.project.hussainproject.drive.dto;

import com.project.hussainproject.drive.model.FileMetadata;
import com.project.hussainproject.drive.model.Folder;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.domain.Page;

@Data
@Builder
public class SearchResponse {
    private Page<Folder> folders;
    private Page<FileMetadata> files;
    private String query;
}

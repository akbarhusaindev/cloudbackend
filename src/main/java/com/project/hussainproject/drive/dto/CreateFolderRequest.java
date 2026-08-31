package com.project.hussainproject.drive.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class CreateFolderRequest {

    private String name;
    private String parentId;
}

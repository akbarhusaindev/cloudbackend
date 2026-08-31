package com.project.hussainproject.drive.dto;

import com.project.hussainproject.drive.model.SharePermission;

import lombok.Data;
import java.util.UUID;

@Data
public class ShareRequest {
    private UUID fileId;
    private String targetEmail;
    private SharePermission permission;
}

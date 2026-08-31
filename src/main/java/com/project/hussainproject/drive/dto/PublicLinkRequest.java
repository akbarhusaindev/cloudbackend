package com.project.hussainproject.drive.dto;

import com.project.hussainproject.drive.model.SharePermission;
import lombok.Data;
import java.util.UUID;

@Data
public class PublicLinkRequest {

    private UUID fileId;
    private SharePermission permission;
    private Integer expiryDays;
    private String password;

}

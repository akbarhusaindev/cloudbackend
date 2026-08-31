package com.project.hussainproject.drive.dto;

import lombok.*;
import lombok.Data;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {
    private String name;
    private String email;
    private String password;
}

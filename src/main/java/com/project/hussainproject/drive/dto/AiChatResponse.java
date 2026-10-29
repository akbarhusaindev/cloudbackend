package com.project.hussainproject.drive.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiChatResponse {
    /** The AI-generated answer or summary text. */
    private String result;
}


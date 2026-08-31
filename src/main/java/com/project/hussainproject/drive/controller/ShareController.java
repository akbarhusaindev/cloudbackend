package com.project.hussainproject.drive.controller;


import com.project.hussainproject.drive.dto.ShareRequest;
import com.project.hussainproject.drive.model.Share;
import com.project.hussainproject.drive.model.User;
import com.project.hussainproject.drive.service.ShareService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shares")
@RequiredArgsConstructor
public class ShareController {
    private final ShareService shareService;

    @PostMapping
    public ResponseEntity<Share> shareFile(
            @RequestBody ShareRequest request,
            @AuthenticationPrincipal User user
            ) {

          return ResponseEntity.ok(shareService.shareFile(request,user));

    }

    @GetMapping("/me")
    public ResponseEntity<List<Share>> getSharedWithMe(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(shareService.getSharedWithMe(user));
    }
}

package com.project.hussainproject.drive.controller;


import com.project.hussainproject.drive.model.User;
import com.project.hussainproject.drive.service.SearchService;
import lombok.RequiredArgsConstructor;
import com.project.hussainproject.drive.dto.SearchResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    public ResponseEntity<SearchResponse> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal User user){
        return ResponseEntity.ok(searchService.search(query, page, size, user));

    }

}

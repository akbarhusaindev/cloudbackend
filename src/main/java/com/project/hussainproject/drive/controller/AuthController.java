package com.project.hussainproject.drive.controller;


import com.project.hussainproject.drive.dto.AuthResponse;
import com.project.hussainproject.drive.dto.LoginRequest;
import com.project.hussainproject.drive.dto.RegisterRequest;
import com.project.hussainproject.drive.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
     public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request){
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> authenticate(@RequestBody LoginRequest request){
        return ResponseEntity.ok(authService.authenticate(request));
    }

}

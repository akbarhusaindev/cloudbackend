package com.project.hussainproject.drive.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.project.hussainproject.drive.dto.AuthResponse;
import com.project.hussainproject.drive.dto.LoginRequest;
import com.project.hussainproject.drive.dto.RegisterRequest;
import com.project.hussainproject.drive.model.Role;
import com.project.hussainproject.drive.model.User;
import com.project.hussainproject.drive.repository.UserRepository;
import com.project.hussainproject.drive.security.JwtService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;


    // REGISTER
    public AuthResponse register(RegisterRequest request) {

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        // Create new user
        var user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .build();

        // Save user to database
        userRepository.save(user);

        // Generate JWT token
        var jwtToken = jwtService.generateToken(user);

        // Return token
        return AuthResponse.builder()
                .token(jwtToken)
                .build();
    }


    // LOGIN
    public AuthResponse authenticate(LoginRequest request) {

        // Authenticate email and password
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // Find user
        var user = userRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Generate JWT token
        var jwtToken = jwtService.generateToken(user);

        // Return token
        return AuthResponse.builder()
                .token(jwtToken)
                .build();
    }
}
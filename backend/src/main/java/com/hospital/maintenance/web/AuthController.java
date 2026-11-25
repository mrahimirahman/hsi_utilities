package com.hospital.maintenance.web;

import com.hospital.maintenance.dto.LoginRequest;
import com.hospital.maintenance.dto.LoginResponse;
import com.hospital.maintenance.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Validated @RequestBody LoginRequest request,
                                               HttpSession session) {
        LoginResponse response = authService.login(request, session);
        if (!response.isAuthenticated()) {
            return new ResponseEntity<LoginResponse>(response, HttpStatus.UNAUTHORIZED);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/session")
    public ResponseEntity<LoginResponse> currentSession(HttpSession session) {
        LoginResponse response = authService.getCurrentSession(session);
        if (!response.isAuthenticated()) {
            return new ResponseEntity<LoginResponse>(response, HttpStatus.UNAUTHORIZED);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpSession session) {
        authService.logout(session);
        return ResponseEntity.noContent().build();
    }
}

package com.hospital.maintenance.service;

import com.hospital.maintenance.domain.SystemUser;
import com.hospital.maintenance.dto.LoginRequest;
import com.hospital.maintenance.dto.LoginResponse;
import com.hospital.maintenance.repository.SystemUserRepository;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;

@Service
public class AuthService {

    public static final String SESSION_USER_KEY = "AUTH_USER";

    private final SystemUserRepository systemUserRepository;

    public AuthService(SystemUserRepository systemUserRepository) {
        this.systemUserRepository = systemUserRepository;
    }

    public LoginResponse login(LoginRequest request, HttpSession session) {
        SystemUser user = systemUserRepository.findByUsername(request.getUsername());
        LoginResponse response = new LoginResponse();

        if (user == null || !user.getPassword().equals(request.getPassword())) {
            response.setAuthenticated(false);
            response.setMessage("Invalid username or password.");
            return response;
        }

        session.setAttribute(SESSION_USER_KEY, user);
        response.setAuthenticated(true);
        response.setMessage("Login successful.");
        response.setUsername(user.getUsername());
        response.setFullName(user.getFullName());
        response.setRole(user.getRole());
        response.setDepartment(user.getDepartment());
        return response;
    }

    public LoginResponse getCurrentSession(HttpSession session) {
        SystemUser user = (SystemUser) session.getAttribute(SESSION_USER_KEY);
        LoginResponse response = new LoginResponse();

        if (user == null) {
            response.setAuthenticated(false);
            response.setMessage("No active session.");
            return response;
        }

        response.setAuthenticated(true);
        response.setMessage("Active session found.");
        response.setUsername(user.getUsername());
        response.setFullName(user.getFullName());
        response.setRole(user.getRole());
        response.setDepartment(user.getDepartment());
        return response;
    }

    public void logout(HttpSession session) {
        session.invalidate();
    }
}

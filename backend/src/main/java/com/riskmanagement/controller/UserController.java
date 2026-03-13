package com.riskmanagement.controller;

import com.riskmanagement.model.User;
import com.riskmanagement.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @GetMapping
    public List<UserResponse> getAllUsers() {
        return userService.getAllUsers().stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(UserResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public UserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        User user = new User();
        user.setInstitutionId(request.getInstitutionId());
        user.setName(request.getName().trim());
        user.setEmail(request.getEmail().trim());
        user.setRole(request.getRole().trim());
        user.setMfaEnabled(request.getMfaEnabled());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setCreatedAt(LocalDateTime.now());
        return UserResponse.from(userService.saveUser(user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        User existing = userService.getUserById(id).orElse(null);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }

        existing.setInstitutionId(request.getInstitutionId());
        existing.setName(request.getName().trim());
        existing.setEmail(request.getEmail().trim());
        existing.setRole(request.getRole().trim());
        existing.setMfaEnabled(request.getMfaEnabled());
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            existing.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        return ResponseEntity.ok(UserResponse.from(userService.saveUser(existing)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        if (!userService.getUserById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    public static class CreateUserRequest {
        @NotNull
        private Long institutionId;

        @NotBlank
        private String name;

        @NotBlank
        @Email
        private String email;

        @NotBlank
        private String role;

        @NotNull
        private Boolean mfaEnabled;

        @NotBlank
        @Size(min = 8)
        private String password;

        public Long getInstitutionId() {
            return institutionId;
        }

        public void setInstitutionId(Long institutionId) {
            this.institutionId = institutionId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public Boolean getMfaEnabled() {
            return mfaEnabled;
        }

        public void setMfaEnabled(Boolean mfaEnabled) {
            this.mfaEnabled = mfaEnabled;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class UpdateUserRequest {
        @NotNull
        private Long institutionId;

        @NotBlank
        private String name;

        @NotBlank
        @Email
        private String email;

        @NotBlank
        private String role;

        @NotNull
        private Boolean mfaEnabled;

        @Size(min = 8)
        private String password;

        public Long getInstitutionId() {
            return institutionId;
        }

        public void setInstitutionId(Long institutionId) {
            this.institutionId = institutionId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public Boolean getMfaEnabled() {
            return mfaEnabled;
        }

        public void setMfaEnabled(Boolean mfaEnabled) {
            this.mfaEnabled = mfaEnabled;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class UserResponse {
        private Long userId;
        private Long institutionId;
        private String name;
        private String email;
        private String role;
        private Boolean mfaEnabled;
        private LocalDateTime createdAt;
        private LocalDateTime lastLogin;

        public static UserResponse from(User user) {
            UserResponse response = new UserResponse();
            response.setUserId(user.getUserId());
            response.setInstitutionId(user.getInstitutionId());
            response.setName(user.getName());
            response.setEmail(user.getEmail());
            response.setRole(user.getRole());
            response.setMfaEnabled(user.getMfaEnabled());
            response.setCreatedAt(user.getCreatedAt());
            response.setLastLogin(user.getLastLogin());
            return response;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public Long getInstitutionId() {
            return institutionId;
        }

        public void setInstitutionId(Long institutionId) {
            this.institutionId = institutionId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public Boolean getMfaEnabled() {
            return mfaEnabled;
        }

        public void setMfaEnabled(Boolean mfaEnabled) {
            this.mfaEnabled = mfaEnabled;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }

        public LocalDateTime getLastLogin() {
            return lastLogin;
        }

        public void setLastLogin(LocalDateTime lastLogin) {
            this.lastLogin = lastLogin;
        }
    }
}
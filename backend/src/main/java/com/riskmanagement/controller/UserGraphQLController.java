package com.riskmanagement.controller;

import com.riskmanagement.model.User;
import com.riskmanagement.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Optional;

@Controller
public class UserGraphQLController {
    @Autowired
    private UserService userService;

    @QueryMapping
    public List<User> users() {
        return userService.getAllUsers();
    }

    @QueryMapping
    public User user(@Argument Long id) {
        return userService.getUserById(id).orElse(null);
    }

    @QueryMapping
    public User userByEmail(@Argument String email) {
        return userService.getUserByEmail(email).orElse(null);
    }

    @MutationMapping
    public User createUser(@Argument UserInput user) {
        User u = toUser(user);
        return userService.saveUser(u);
    }

    @MutationMapping
    public User updateUser(@Argument Long id, @Argument UserInput user) {
        Optional<User> existing = userService.getUserById(id);
        if (existing.isEmpty()) return null;
        User u = toUser(user);
        u.setUserId(id);
        return userService.saveUser(u);
    }

    @MutationMapping
    public Boolean deleteUser(@Argument Long id) {
        if (userService.getUserById(id).isEmpty()) return false;
        userService.deleteUser(id);
        return true;
    }

    public static class UserInput {
        public Long institutionId;
        public String name;
        public String email;
        public String role;
        public String password;
        public Boolean mfaEnabled;
    }

    private User toUser(UserInput input) {
        User u = new User();
        u.setInstitutionId(input.institutionId);
        u.setName(input.name);
        u.setEmail(input.email);
        u.setRole(input.role);
        // Password handling: hash if present
        if (input.password != null && !input.password.isEmpty()) {
            // You may want to inject a password encoder here
            u.setPasswordHash(input.password); // Replace with encoder.encode(input.password) in real code
        }
        u.setMfaEnabled(input.mfaEnabled);
        return u;
    }
}

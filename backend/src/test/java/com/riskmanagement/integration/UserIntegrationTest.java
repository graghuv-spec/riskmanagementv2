package com.riskmanagement.integration;

import com.riskmanagement.model.Institution;
import com.riskmanagement.model.User;
import com.riskmanagement.service.InstitutionService;
import com.riskmanagement.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
class UserIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private InstitutionService institutionService;

    private Institution savedInstitution() {
        Institution inst = new Institution();
        inst.setName("User Test Bank");
        inst.setLicenseNumber("LIC-USER");
        inst.setContactEmail("user@test.com");
        inst.setSubscriptionPlan("Standard");
        inst.setCreatedAt(LocalDateTime.now());
        return institutionService.saveInstitution(inst);
    }

    private User buildUser(String email, Long institutionId) {
        User u = new User();
        u.setName("Test User");
        u.setEmail(email);
        u.setRole("LoanOfficer");
        u.setPasswordHash("hashed_password_123");
        u.setMfaEnabled(false);
        u.setInstitutionId(institutionId);
        u.setCreatedAt(LocalDateTime.now());
        return u;
    }

    @Test
    void testCreate() {
        Institution inst = savedInstitution();
        User saved = userService.saveUser(buildUser("officer@bank.com", inst.getInstitutionId()));
        assertNotNull(saved.getUserId());
        assertEquals("officer@bank.com", saved.getEmail());
        assertEquals("LoanOfficer", saved.getRole());
    }

    @Test
    void testRead() {
        Institution inst = savedInstitution();
        User saved = userService.saveUser(buildUser("read@bank.com", inst.getInstitutionId()));
        Optional<User> found = userService.getUserById(saved.getUserId());
        assertTrue(found.isPresent());
        assertEquals("Test User", found.get().getName());
        assertFalse(found.get().getMfaEnabled());
    }

    @Test
    void testUpdate() {
        Institution inst = savedInstitution();
        User saved = userService.saveUser(buildUser("update@bank.com", inst.getInstitutionId()));
        saved.setRole("RiskManager");
        saved.setMfaEnabled(true);
        User updated = userService.saveUser(saved);
        assertEquals("RiskManager", updated.getRole());
        assertTrue(updated.getMfaEnabled());
    }

    @Test
    void testDelete() {
        Institution inst = savedInstitution();
        User saved = userService.saveUser(buildUser("delete@bank.com", inst.getInstitutionId()));
        Long id = saved.getUserId();
        userService.deleteUser(id);
        assertFalse(userService.getUserById(id).isPresent());
    }

    @Test
    void testGetAll() {
        Institution inst = savedInstitution();
        int before = userService.getAllUsers().size();
        userService.saveUser(buildUser("user1@bank.com", inst.getInstitutionId()));
        userService.saveUser(buildUser("user2@bank.com", inst.getInstitutionId()));
        List<User> all = userService.getAllUsers();
        assertEquals(before + 2, all.size());
    }
}

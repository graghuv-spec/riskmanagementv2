package com.riskmanagement.integration;

import com.riskmanagement.model.Institution;
import com.riskmanagement.service.InstitutionService;
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
class InstitutionIntegrationTest {

    @Autowired
    private InstitutionService institutionService;

    private Institution buildInstitution(String name) {
        Institution inst = new Institution();
        inst.setName(name);
        inst.setLicenseNumber("LIC-" + name);
        inst.setContactEmail(name.toLowerCase() + "@test.com");
        inst.setSubscriptionPlan("Standard");
        inst.setCreatedAt(LocalDateTime.now());
        return inst;
    }

    @Test
    void testCreate() {
        Institution saved = institutionService.saveInstitution(buildInstitution("Alpha Bank"));
        assertNotNull(saved.getInstitutionId());
        assertEquals("Alpha Bank", saved.getName());
        assertEquals("LIC-Alpha Bank", saved.getLicenseNumber());
    }

    @Test
    void testRead() {
        Institution saved = institutionService.saveInstitution(buildInstitution("Beta Bank"));
        Optional<Institution> found = institutionService.getInstitutionById(saved.getInstitutionId());
        assertTrue(found.isPresent());
        assertEquals("beta bank@test.com", found.get().getContactEmail());
    }

    @Test
    void testUpdate() {
        Institution saved = institutionService.saveInstitution(buildInstitution("Gamma Bank"));
        saved.setSubscriptionPlan("Premium");
        Institution updated = institutionService.saveInstitution(saved);
        assertEquals("Premium", updated.getSubscriptionPlan());
        assertEquals(saved.getInstitutionId(), updated.getInstitutionId());
    }

    @Test
    void testDelete() {
        Institution saved = institutionService.saveInstitution(buildInstitution("Delta Bank"));
        Long id = saved.getInstitutionId();
        institutionService.deleteInstitution(id);
        Optional<Institution> found = institutionService.getInstitutionById(id);
        assertFalse(found.isPresent());
    }

    @Test
    void testGetAll() {
        int before = institutionService.getAllInstitutions().size();
        institutionService.saveInstitution(buildInstitution("Epsilon Bank"));
        institutionService.saveInstitution(buildInstitution("Zeta Bank"));
        List<Institution> all = institutionService.getAllInstitutions();
        assertEquals(before + 2, all.size());
    }
}

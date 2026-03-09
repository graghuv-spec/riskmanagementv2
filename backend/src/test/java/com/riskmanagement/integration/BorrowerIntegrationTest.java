package com.riskmanagement.integration;

import com.riskmanagement.model.Borrower;
import com.riskmanagement.model.Institution;
import com.riskmanagement.service.BorrowerService;
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
class BorrowerIntegrationTest {

    @Autowired
    private BorrowerService borrowerService;

    @Autowired
    private InstitutionService institutionService;

    private Institution savedInstitution() {
        Institution inst = new Institution();
        inst.setName("Test Institution");
        inst.setLicenseNumber("LIC-TEST");
        inst.setContactEmail("inst@test.com");
        inst.setSubscriptionPlan("Standard");
        inst.setCreatedAt(LocalDateTime.now());
        return institutionService.saveInstitution(inst);
    }

    private Borrower buildBorrower(String name, Long institutionId) {
        Borrower b = new Borrower();
        b.setFullName(name);
        b.setNationalId("NID-" + name);
        b.setGender("Male");
        b.setAge(35);
        b.setLocation("Nairobi");
        b.setBusinessSector("Agriculture");
        b.setMonthlyIncome(3000.0);
        b.setCollateralValue(15000.0);
        b.setInstitutionId(institutionId);
        b.setCreatedAt(LocalDateTime.now());
        return b;
    }

    @Test
    void testCreate() {
        Institution inst = savedInstitution();
        Borrower saved = borrowerService.saveBorrower(buildBorrower("John Doe", inst.getInstitutionId()));
        assertNotNull(saved.getBorrowerId());
        assertEquals("John Doe", saved.getFullName());
        assertEquals(inst.getInstitutionId(), saved.getInstitutionId());
    }

    @Test
    void testRead() {
        Institution inst = savedInstitution();
        Borrower saved = borrowerService.saveBorrower(buildBorrower("Jane Doe", inst.getInstitutionId()));
        Optional<Borrower> found = borrowerService.getBorrowerById(saved.getBorrowerId());
        assertTrue(found.isPresent());
        assertEquals("Nairobi", found.get().getLocation());
        assertEquals(3000.0, found.get().getMonthlyIncome());
    }

    @Test
    void testUpdate() {
        Institution inst = savedInstitution();
        Borrower saved = borrowerService.saveBorrower(buildBorrower("James Smith", inst.getInstitutionId()));
        saved.setMonthlyIncome(5000.0);
        saved.setLocation("Mombasa");
        Borrower updated = borrowerService.saveBorrower(saved);
        assertEquals(5000.0, updated.getMonthlyIncome());
        assertEquals("Mombasa", updated.getLocation());
    }

    @Test
    void testDelete() {
        Institution inst = savedInstitution();
        Borrower saved = borrowerService.saveBorrower(buildBorrower("Mark Lee", inst.getInstitutionId()));
        Long id = saved.getBorrowerId();
        borrowerService.deleteBorrower(id);
        assertFalse(borrowerService.getBorrowerById(id).isPresent());
    }

    @Test
    void testGetAll() {
        Institution inst = savedInstitution();
        int before = borrowerService.getAllBorrowers().size();
        borrowerService.saveBorrower(buildBorrower("Alice", inst.getInstitutionId()));
        borrowerService.saveBorrower(buildBorrower("Bob", inst.getInstitutionId()));
        List<Borrower> all = borrowerService.getAllBorrowers();
        assertEquals(before + 2, all.size());
    }
}

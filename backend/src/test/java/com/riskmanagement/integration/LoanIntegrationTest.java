package com.riskmanagement.integration;

import com.riskmanagement.model.Borrower;
import com.riskmanagement.model.Institution;
import com.riskmanagement.model.Loan;
import com.riskmanagement.service.BorrowerService;
import com.riskmanagement.service.InstitutionService;
import com.riskmanagement.service.LoanService;
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
class LoanIntegrationTest {

    @Autowired
    private LoanService loanService;

    @Autowired
    private BorrowerService borrowerService;

    @Autowired
    private InstitutionService institutionService;

    private Institution savedInstitution() {
        Institution inst = new Institution();
        inst.setName("Loan Test Bank");
        inst.setLicenseNumber("LIC-LOAN");
        inst.setContactEmail("loan@test.com");
        inst.setSubscriptionPlan("Standard");
        inst.setCreatedAt(LocalDateTime.now());
        return institutionService.saveInstitution(inst);
    }

    private Borrower savedBorrower(Long institutionId) {
        Borrower b = new Borrower();
        b.setFullName("Loan Borrower");
        b.setNationalId("NID-LOAN");
        b.setGender("Female");
        b.setAge(28);
        b.setLocation("Kampala");
        b.setBusinessSector("Finance");
        b.setMonthlyIncome(4500.0);
        b.setCollateralValue(20000.0);
        b.setInstitutionId(institutionId);
        b.setCreatedAt(LocalDateTime.now());
        return borrowerService.saveBorrower(b);
    }

    private Loan buildLoan(Long borrowerId, Long institutionId) {
        Loan loan = new Loan();
        loan.setBorrowerId(borrowerId);
        loan.setInstitutionId(institutionId);
        loan.setLoanAmount(10000.0);
        loan.setInterestRate(12.5);
        loan.setTenureMonths(24);
        loan.setDisbursementDate(LocalDateTime.now());
        loan.setStatus("Active");
        loan.setCreatedAt(LocalDateTime.now());
        return loan;
    }

    @Test
    void testCreate() {
        Institution inst = savedInstitution();
        Borrower borrower = savedBorrower(inst.getInstitutionId());
        Loan saved = loanService.saveLoan(buildLoan(borrower.getBorrowerId(), inst.getInstitutionId()));
        assertNotNull(saved.getLoanId());
        assertEquals(10000.0, saved.getLoanAmount());
        assertEquals("Active", saved.getStatus());
    }

    @Test
    void testRead() {
        Institution inst = savedInstitution();
        Borrower borrower = savedBorrower(inst.getInstitutionId());
        Loan saved = loanService.saveLoan(buildLoan(borrower.getBorrowerId(), inst.getInstitutionId()));
        Optional<Loan> found = loanService.getLoanById(saved.getLoanId());
        assertTrue(found.isPresent());
        assertEquals(12.5, found.get().getInterestRate());
        assertEquals(24, found.get().getTenureMonths());
    }

    @Test
    void testUpdate() {
        Institution inst = savedInstitution();
        Borrower borrower = savedBorrower(inst.getInstitutionId());
        Loan saved = loanService.saveLoan(buildLoan(borrower.getBorrowerId(), inst.getInstitutionId()));
        saved.setStatus("Closed");
        saved.setLoanAmount(9500.0);
        Loan updated = loanService.saveLoan(saved);
        assertEquals("Closed", updated.getStatus());
        assertEquals(9500.0, updated.getLoanAmount());
    }

    @Test
    void testDelete() {
        Institution inst = savedInstitution();
        Borrower borrower = savedBorrower(inst.getInstitutionId());
        Loan saved = loanService.saveLoan(buildLoan(borrower.getBorrowerId(), inst.getInstitutionId()));
        Long id = saved.getLoanId();
        loanService.deleteLoan(id);
        assertFalse(loanService.getLoanById(id).isPresent());
    }

    @Test
    void testGetAll() {
        Institution inst = savedInstitution();
        Borrower borrower = savedBorrower(inst.getInstitutionId());
        int before = loanService.getAllLoans().size();
        loanService.saveLoan(buildLoan(borrower.getBorrowerId(), inst.getInstitutionId()));
        loanService.saveLoan(buildLoan(borrower.getBorrowerId(), inst.getInstitutionId()));
        List<Loan> all = loanService.getAllLoans();
        assertEquals(before + 2, all.size());
    }
}

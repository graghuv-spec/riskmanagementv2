package com.riskmanagement.integration;

import com.riskmanagement.model.*;
import com.riskmanagement.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end integration test covering the complete risk management flow:
 * Institution → User → Borrower → Loan → Repayment → RiskScore → AuditLog → PortfolioMetrics
 *
 * All data is rolled back after each test via @Transactional.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
class CompleteFlowIntegrationTest {

    @Autowired
    private InstitutionService institutionService;
    @Autowired
    private UserService userService;
    @Autowired
    private BorrowerService borrowerService;
    @Autowired
    private LoanService loanService;
    @Autowired
    private RepaymentService repaymentService;
    @Autowired
    private RiskCalculationService riskCalculationService;
    @Autowired
    private RiskScoreService riskScoreService;
    @Autowired
    private AuditLogService auditLogService;
    @Autowired
    private PortfolioMetricsService portfolioMetricsService;

    @Test
    void testCompleteRiskAssessmentFlow() {

        // ── Step 1: Create institution ────────────────────────────────────────────
        Institution institution = new Institution();
        institution.setName("Micro Finance Bank");
        institution.setLicenseNumber("LIC-MFB-001");
        institution.setContactEmail("admin@mfb.com");
        institution.setSubscriptionPlan("Premium");
        institution.setCreatedAt(LocalDateTime.now());
        institution = institutionService.saveInstitution(institution);
        assertNotNull(institution.getInstitutionId(), "Institution should be persisted");

        // ── Step 2: Create user linked to institution ─────────────────────────────
        User user = new User();
        user.setName("Risk Manager Alice");
        user.setEmail("alice@mfb.com");
        user.setRole("RiskManager");
        user.setPasswordHash("bcrypt_hash_placeholder");
        user.setMfaEnabled(true);
        user.setInstitutionId(institution.getInstitutionId());
        user.setCreatedAt(LocalDateTime.now());
        user = userService.saveUser(user);
        assertNotNull(user.getUserId(), "User should be persisted");
        assertEquals("RiskManager", user.getRole());

        // ── Step 3: Create borrower linked to institution ─────────────────────────
        Borrower borrower = new Borrower();
        borrower.setFullName("Samuel Omondi");
        borrower.setNationalId("NID-20240001");
        borrower.setGender("Male");
        borrower.setAge(32);
        borrower.setLocation("Nairobi");
        borrower.setBusinessSector("Technology");
        borrower.setMonthlyIncome(5500.0);
        borrower.setCollateralValue(25000.0);
        borrower.setInstitutionId(institution.getInstitutionId());
        borrower.setCreatedAt(LocalDateTime.now());
        borrower = borrowerService.saveBorrower(borrower);
        assertNotNull(borrower.getBorrowerId(), "Borrower should be persisted");
        assertEquals("Samuel Omondi", borrower.getFullName());

        // ── Step 4: Create loan linked to borrower and institution ────────────────
        Loan loan = new Loan();
        loan.setBorrowerId(borrower.getBorrowerId());
        loan.setInstitutionId(institution.getInstitutionId());
        loan.setLoanAmount(15000.0);
        loan.setInterestRate(14.0);
        loan.setTenureMonths(18);
        loan.setDisbursementDate(LocalDateTime.now());
        loan.setStatus("Active");
        loan.setCreatedAt(LocalDateTime.now());
        loan = loanService.saveLoan(loan);
        assertNotNull(loan.getLoanId(), "Loan should be persisted");
        assertEquals("Active", loan.getStatus());
        assertEquals(borrower.getBorrowerId(), loan.getBorrowerId());

        // ── Step 5: Create repayments for the loan ────────────────────────────────
        Repayment repayment1 = new Repayment();
        repayment1.setLoanId(loan.getLoanId());
        repayment1.setDueDate(LocalDate.now().plusMonths(1));
        repayment1.setPaymentDate(LocalDate.now());
        repayment1.setAmountDue(900.0);
        repayment1.setAmountPaid(900.0);
        repayment1.setDaysPastDue(0);
        repayment1 = repaymentService.saveRepayment(repayment1);
        assertNotNull(repayment1.getRepaymentId(), "Repayment 1 should be persisted");

        Repayment repayment2 = new Repayment();
        repayment2.setLoanId(loan.getLoanId());
        repayment2.setDueDate(LocalDate.now().plusMonths(2));
        repayment2.setAmountDue(900.0);
        repayment2.setAmountPaid(0.0);
        repayment2.setDaysPastDue(0);
        repayment2 = repaymentService.saveRepayment(repayment2);
        assertNotNull(repayment2.getRepaymentId(), "Repayment 2 should be persisted");

        // ── Step 6: Calculate and persist risk score ──────────────────────────────
        RiskScore riskScore = riskCalculationService.calculateRiskScore(loan, borrower);
        assertNotNull(riskScore, "Risk score should be calculated");
        assertNotNull(riskScore.getRiskScore());
        assertNotNull(riskScore.getRiskGrade());
        assertNotNull(riskScore.getProbabilityDefault());
        assertTrue(riskScore.getRiskScore() >= 0 && riskScore.getRiskScore() <= 100,
                "Risk score should be in 0-100 range, got: " + riskScore.getRiskScore());

        riskScore = riskScoreService.saveRiskScore(riskScore);
        assertNotNull(riskScore.getRiskId(), "Risk score should be persisted");
        assertEquals(loan.getLoanId(), riskScore.getLoanId());
        assertEquals("Rule-Based v1", riskScore.getModelVersion());

        // ── Step 7: Log audit trail ───────────────────────────────────────────────
        AuditLog auditLog = new AuditLog();
        auditLog.setUserId(user.getUserId());
        auditLog.setAction("RISK_SCORE_CALCULATED");
        auditLog.setEntityType("RiskScore");
        auditLog.setEntityId(riskScore.getRiskId());
        auditLog.setTimestamp(LocalDateTime.now());
        auditLog.setIpAddress("10.0.0.1");
        auditLog = auditLogService.saveAuditLog(auditLog);
        assertNotNull(auditLog.getLogId(), "Audit log should be persisted");

        // ── Step 8: Generate portfolio metrics ────────────────────────────────────
        PortfolioMetrics metrics = new PortfolioMetrics();
        metrics.setInstitutionId(institution.getInstitutionId());
        metrics.setPar30(2.5);
        metrics.setPar90(0.8);
        metrics.setSectorConcentration(0.40);
        metrics.setRegionRiskIndex(0.55);
        metrics.setForecastDefaultRate(0.06);
        metrics.setGeneratedAt(LocalDateTime.now());
        metrics = portfolioMetricsService.savePortfolioMetrics(metrics);
        assertNotNull(metrics.getMetricId(), "Portfolio metrics should be persisted");

        // ── Step 9: Update entities ───────────────────────────────────────────────
        loan.setStatus("Closed");
        loan = loanService.saveLoan(loan);
        assertEquals("Closed", loan.getStatus());

        repayment2.setAmountPaid(900.0);
        repayment2.setPaymentDate(LocalDate.now());
        repayment2 = repaymentService.saveRepayment(repayment2);
        assertEquals(900.0, repayment2.getAmountPaid());

        borrower.setMonthlyIncome(6000.0);
        borrower = borrowerService.saveBorrower(borrower);
        assertEquals(6000.0, borrower.getMonthlyIncome());

        // ── Step 10: Verify all data retrievable ──────────────────────────────────
        assertTrue(institutionService.getInstitutionById(institution.getInstitutionId()).isPresent());
        assertTrue(userService.getUserById(user.getUserId()).isPresent());
        assertTrue(borrowerService.getBorrowerById(borrower.getBorrowerId()).isPresent());
        assertTrue(loanService.getLoanById(loan.getLoanId()).isPresent());
        assertTrue(repaymentService.getRepaymentById(repayment1.getRepaymentId()).isPresent());
        assertTrue(repaymentService.getRepaymentById(repayment2.getRepaymentId()).isPresent());
        assertTrue(riskScoreService.getRiskScoreById(riskScore.getRiskId()).isPresent());
        assertTrue(auditLogService.getAuditLogById(auditLog.getLogId()).isPresent());
        assertTrue(portfolioMetricsService.getPortfolioMetricsById(metrics.getMetricId()).isPresent());

        // ── Step 11: Delete entities in reverse dependency order ──────────────────
        portfolioMetricsService.deletePortfolioMetrics(metrics.getMetricId());
        auditLogService.deleteAuditLog(auditLog.getLogId());
        riskScoreService.deleteRiskScore(riskScore.getRiskId());
        repaymentService.deleteRepayment(repayment2.getRepaymentId());
        repaymentService.deleteRepayment(repayment1.getRepaymentId());
        loanService.deleteLoan(loan.getLoanId());
        borrowerService.deleteBorrower(borrower.getBorrowerId());
        userService.deleteUser(user.getUserId());
        institutionService.deleteInstitution(institution.getInstitutionId());

        // ── Step 12: Verify all deleted ───────────────────────────────────────────
        assertFalse(portfolioMetricsService.getPortfolioMetricsById(metrics.getMetricId()).isPresent());
        assertFalse(auditLogService.getAuditLogById(auditLog.getLogId()).isPresent());
        assertFalse(riskScoreService.getRiskScoreById(riskScore.getRiskId()).isPresent());
        assertFalse(repaymentService.getRepaymentById(repayment2.getRepaymentId()).isPresent());
        assertFalse(repaymentService.getRepaymentById(repayment1.getRepaymentId()).isPresent());
        assertFalse(loanService.getLoanById(loan.getLoanId()).isPresent());
        assertFalse(borrowerService.getBorrowerById(borrower.getBorrowerId()).isPresent());
        assertFalse(userService.getUserById(user.getUserId()).isPresent());
        assertFalse(institutionService.getInstitutionById(institution.getInstitutionId()).isPresent());
    }
}

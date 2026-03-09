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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
class RiskCalculationIntegrationTest {

    @Autowired
    private RiskCalculationService riskCalculationService;

    @Autowired
    private RiskScoreService riskScoreService;

    @Autowired
    private LoanService loanService;

    @Autowired
    private BorrowerService borrowerService;

    @Autowired
    private RepaymentService repaymentService;

    private Borrower buildBorrower(double income, double collateral, String sector, String location) {
        Borrower b = new Borrower();
        b.setFullName("Risk Test Borrower");
        b.setNationalId("NID-RISK");
        b.setGender("Male");
        b.setAge(40);
        b.setLocation(location);
        b.setBusinessSector(sector);
        b.setMonthlyIncome(income);
        b.setCollateralValue(collateral);
        b.setCreatedAt(LocalDateTime.now());
        return b;
    }

    private Loan buildLoan(Long borrowerId, double amount) {
        Loan loan = new Loan();
        loan.setBorrowerId(borrowerId);
        loan.setLoanAmount(amount);
        loan.setInterestRate(15.0);
        loan.setTenureMonths(12);
        loan.setStatus("Active");
        loan.setCreatedAt(LocalDateTime.now());
        return loan;
    }

    @Test
    void testRiskScoreIsCalculatedAndPersisted() {
        Borrower borrower = borrowerService.saveBorrower(buildBorrower(6000.0, 30000.0, "Technology", "Nairobi"));
        Loan loan = loanService.saveLoan(buildLoan(borrower.getBorrowerId(), 10000.0));

        RiskScore riskScore = riskCalculationService.calculateRiskScore(loan, borrower);
        assertNotNull(riskScore);

        RiskScore saved = riskScoreService.saveRiskScore(riskScore);
        assertNotNull(saved.getRiskId());
        assertNotNull(saved.getRiskScore());
        assertNotNull(saved.getRiskGrade());
        assertNotNull(saved.getProbabilityDefault());
        assertEquals(loan.getLoanId(), saved.getLoanId());
    }

    @Test
    void testHighIncomeHighCollateralProducesHighScore() {
        Borrower borrower = borrowerService.saveBorrower(buildBorrower(10000.0, 50000.0, "Finance", "Nairobi"));
        Loan loan = loanService.saveLoan(buildLoan(borrower.getBorrowerId(), 10000.0));

        RiskScore score = riskCalculationService.calculateRiskScore(loan, borrower);
        assertTrue(score.getRiskScore() >= 60, "High income + high collateral should produce a good risk score");
        assertTrue(score.getRiskGrade().equals("A") || score.getRiskGrade().equals("B"),
                "Grade should be A or B, got: " + score.getRiskGrade());
    }

    @Test
    void testLowIncomeProducesLowerScore() {
        Borrower poorBorrower = borrowerService.saveBorrower(buildBorrower(500.0, 1000.0, "Agriculture", "Rural"));
        Loan loan = loanService.saveLoan(buildLoan(poorBorrower.getBorrowerId(), 10000.0));

        RiskScore score = riskCalculationService.calculateRiskScore(loan, poorBorrower);
        assertTrue(score.getRiskScore() < 80, "Low income should produce a lower risk score");
    }

    @Test
    void testRiskScoreWithLatePastDueRepayment() {
        Borrower borrower = borrowerService.saveBorrower(buildBorrower(3000.0, 10000.0, "Agriculture", "Nairobi"));
        Loan loan = loanService.saveLoan(buildLoan(borrower.getBorrowerId(), 8000.0));

        Repayment lateRepayment = new Repayment();
        lateRepayment.setLoanId(loan.getLoanId());
        lateRepayment.setDueDate(LocalDate.now().minusDays(30));
        lateRepayment.setAmountDue(700.0);
        lateRepayment.setAmountPaid(400.0);
        lateRepayment.setDaysPastDue(30);
        repaymentService.saveRepayment(lateRepayment);

        RiskScore score = riskCalculationService.calculateRiskScore(loan, borrower);
        assertNotNull(score);
        assertNotNull(score.getRiskGrade());
    }

    @Test
    void testModelVersionIsSet() {
        Borrower borrower = borrowerService.saveBorrower(buildBorrower(4000.0, 20000.0, "Technology", "Kampala"));
        Loan loan = loanService.saveLoan(buildLoan(borrower.getBorrowerId(), 5000.0));
        RiskScore score = riskCalculationService.calculateRiskScore(loan, borrower);
        assertEquals("Rule-Based v1", score.getModelVersion());
    }

    @Test
    void testRiskScoreCRUD() {
        Borrower borrower = borrowerService.saveBorrower(buildBorrower(5000.0, 25000.0, "Finance", "Nairobi"));
        Loan loan = loanService.saveLoan(buildLoan(borrower.getBorrowerId(), 10000.0));

        RiskScore calculated = riskCalculationService.calculateRiskScore(loan, borrower);
        RiskScore saved = riskScoreService.saveRiskScore(calculated);
        assertNotNull(saved.getRiskId());

        saved.setRiskGrade("A");
        RiskScore updated = riskScoreService.saveRiskScore(saved);
        assertEquals("A", updated.getRiskGrade());

        riskScoreService.deleteRiskScore(updated.getRiskId());
        assertFalse(riskScoreService.getRiskScoreById(updated.getRiskId()).isPresent());
    }
}

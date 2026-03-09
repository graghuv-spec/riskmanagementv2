package com.riskmanagement.integration;

import com.riskmanagement.model.Loan;
import com.riskmanagement.model.Repayment;
import com.riskmanagement.service.LoanService;
import com.riskmanagement.service.RepaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
class RepaymentIntegrationTest {

    @Autowired
    private RepaymentService repaymentService;

    @Autowired
    private LoanService loanService;

    private Loan savedLoan() {
        Loan loan = new Loan();
        loan.setLoanAmount(8000.0);
        loan.setInterestRate(10.0);
        loan.setTenureMonths(12);
        loan.setStatus("Active");
        loan.setCreatedAt(LocalDateTime.now());
        return loanService.saveLoan(loan);
    }

    private Repayment buildRepayment(Long loanId) {
        Repayment r = new Repayment();
        r.setLoanId(loanId);
        r.setDueDate(LocalDate.now().plusMonths(1));
        r.setPaymentDate(LocalDate.now());
        r.setAmountDue(700.0);
        r.setAmountPaid(700.0);
        r.setDaysPastDue(0);
        return r;
    }

    @Test
    void testCreate() {
        Loan loan = savedLoan();
        Repayment saved = repaymentService.saveRepayment(buildRepayment(loan.getLoanId()));
        assertNotNull(saved.getRepaymentId());
        assertEquals(loan.getLoanId(), saved.getLoanId());
        assertEquals(700.0, saved.getAmountDue());
    }

    @Test
    void testRead() {
        Loan loan = savedLoan();
        Repayment saved = repaymentService.saveRepayment(buildRepayment(loan.getLoanId()));
        Optional<Repayment> found = repaymentService.getRepaymentById(saved.getRepaymentId());
        assertTrue(found.isPresent());
        assertEquals(700.0, found.get().getAmountPaid());
        assertEquals(0, found.get().getDaysPastDue());
    }

    @Test
    void testUpdate() {
        Loan loan = savedLoan();
        Repayment saved = repaymentService.saveRepayment(buildRepayment(loan.getLoanId()));
        saved.setAmountPaid(500.0);
        saved.setDaysPastDue(15);
        Repayment updated = repaymentService.saveRepayment(saved);
        assertEquals(500.0, updated.getAmountPaid());
        assertEquals(15, updated.getDaysPastDue());
    }

    @Test
    void testDelete() {
        Loan loan = savedLoan();
        Repayment saved = repaymentService.saveRepayment(buildRepayment(loan.getLoanId()));
        Long id = saved.getRepaymentId();
        repaymentService.deleteRepayment(id);
        assertFalse(repaymentService.getRepaymentById(id).isPresent());
    }

    @Test
    void testGetAll() {
        Loan loan = savedLoan();
        int before = repaymentService.getAllRepayments().size();
        repaymentService.saveRepayment(buildRepayment(loan.getLoanId()));
        repaymentService.saveRepayment(buildRepayment(loan.getLoanId()));
        List<Repayment> all = repaymentService.getAllRepayments();
        assertEquals(before + 2, all.size());
    }
}

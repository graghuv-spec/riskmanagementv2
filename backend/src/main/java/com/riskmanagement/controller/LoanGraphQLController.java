package com.riskmanagement.controller;

import com.riskmanagement.model.Loan;
import com.riskmanagement.service.LoanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Controller
public class LoanGraphQLController {

    @Autowired
    private LoanService loanService;

    @QueryMapping
    public List<Loan> loans() {
        return loanService.getAllLoans();
    }

    @QueryMapping
    public Optional<Loan> loan(@Argument Long id) {
        return loanService.getLoanById(id);
    }

    @MutationMapping
    public Loan createLoan(@Argument LoanInput loanInput) {
        Loan loan = new Loan();
        loan.setBorrowerId(loanInput.getBorrowerId());
        loan.setInstitutionId(loanInput.getInstitutionId());
        loan.setLoanAmount(loanInput.getLoanAmount());
        loan.setInterestRate(loanInput.getInterestRate());
        loan.setTenureMonths(loanInput.getTenureMonths());
        loan.setDisbursementDate(loanInput.getDisbursementDate());
        loan.setStatus(loanInput.getStatus());
        loan.setCreatedAt(LocalDateTime.now());
        return loanService.saveLoan(loan);
    }

    @MutationMapping
    public Loan updateLoan(@Argument Long id, @Argument LoanInput loanInput) {
        Optional<Loan> existing = loanService.getLoanById(id);
        if (existing.isPresent()) {
            Loan loan = existing.get();
            loan.setBorrowerId(loanInput.getBorrowerId());
            loan.setInstitutionId(loanInput.getInstitutionId());
            loan.setLoanAmount(loanInput.getLoanAmount());
            loan.setInterestRate(loanInput.getInterestRate());
            loan.setTenureMonths(loanInput.getTenureMonths());
            loan.setDisbursementDate(loanInput.getDisbursementDate());
            loan.setStatus(loanInput.getStatus());
            return loanService.saveLoan(loan);
        }
        return null;
    }

    @MutationMapping
    public Boolean deleteLoan(@Argument Long id) {
        if (loanService.getLoanById(id).isPresent()) {
            loanService.deleteLoan(id);
            return true;
        }
        return false;
    }

    // Input class
    public static class LoanInput {
        private Long borrowerId;
        private Long institutionId;
        private Double loanAmount;
        private Double interestRate;
        private Integer tenureMonths;
        private LocalDateTime disbursementDate;
        private String status;

        // getters and setters
        public Long getBorrowerId() {
            return borrowerId;
        }

        public void setBorrowerId(Long borrowerId) {
            this.borrowerId = borrowerId;
        }

        public Long getInstitutionId() {
            return institutionId;
        }

        public void setInstitutionId(Long institutionId) {
            this.institutionId = institutionId;
        }

        public Double getLoanAmount() {
            return loanAmount;
        }

        public void setLoanAmount(Double loanAmount) {
            this.loanAmount = loanAmount;
        }

        public Double getInterestRate() {
            return interestRate;
        }

        public void setInterestRate(Double interestRate) {
            this.interestRate = interestRate;
        }

        public Integer getTenureMonths() {
            return tenureMonths;
        }

        public void setTenureMonths(Integer tenureMonths) {
            this.tenureMonths = tenureMonths;
        }

        public LocalDateTime getDisbursementDate() {
            return disbursementDate;
        }

        public void setDisbursementDate(LocalDateTime disbursementDate) {
            this.disbursementDate = disbursementDate;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}
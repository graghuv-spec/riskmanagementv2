package com.riskmanagement.controller;

import com.riskmanagement.model.Loan;
import com.riskmanagement.model.RiskScore;
import com.riskmanagement.repository.RiskScoreRepository;
import com.riskmanagement.service.LoanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

@Controller
public class LoanGraphQLController {

    @Autowired
    private LoanService loanService;

    @Autowired
    private RiskScoreRepository riskScoreRepository;

    @QueryMapping
    public List<Loan> loans() {
        return loanService.getAllLoans();
    }

    @QueryMapping
    public Optional<Loan> loan(@Argument("id") Long id) {
        return loanService.getLoanById(id);
    }

    @MutationMapping
    public Loan createLoan(@Argument("loan") LoanInput loanInput) {
        validateLoanInput(loanInput);
        Loan loan = new Loan();
        loan.setBorrowerId(loanInput.getBorrowerId());
        loan.setInstitutionId(loanInput.getInstitutionId());
        loan.setLoanAmount(loanInput.getLoanAmount());
        loan.setInterestRate(loanInput.getInterestRate());
        loan.setTenureMonths(loanInput.getTenureMonths());
        loan.setDisbursementDate(parseDateTime(loanInput.getDisbursementDate()));
        loan.setStatus(loanInput.getStatus());
        loan.setCreatedAt(LocalDateTime.now());
        return loanService.saveLoan(loan);
    }

    @MutationMapping
    public Loan updateLoan(@Argument("id") Long id, @Argument("loan") LoanInput loanInput) {
        validateLoanInput(loanInput);
        Loan loan = loanService.getLoanById(id)
                .orElseThrow(() -> new RuntimeException("Loan not found with id: " + id));
        loan.setBorrowerId(loanInput.getBorrowerId());
        loan.setInstitutionId(loanInput.getInstitutionId());
        loan.setLoanAmount(loanInput.getLoanAmount());
        loan.setInterestRate(loanInput.getInterestRate());
        loan.setTenureMonths(loanInput.getTenureMonths());
        loan.setDisbursementDate(parseDateTime(loanInput.getDisbursementDate()));
        loan.setStatus(loanInput.getStatus());
        return loanService.saveLoan(loan);
    }

    @MutationMapping
    public Boolean deleteLoan(@Argument("id") Long id) {
        if (loanService.getLoanById(id).isPresent()) {
            loanService.deleteLoan(id);
            return true;
        }
        return false;
    }

    @SchemaMapping(typeName = "Loan", field = "riskScore")
    public RiskScore riskScore(Loan loan) {
        if (loan.getLoanId() == null) return null;
        return riskScoreRepository.findByLoanId(loan.getLoanId()).orElse(null);
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) return null;
        String trimmed = value.trim();
        try {
            if (trimmed.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return LocalDateTime.parse(trimmed + "T00:00:00");
            }
            return LocalDateTime.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("disbursementDate must be a valid ISO date (YYYY-MM-DD or YYYY-MM-DDTHH:MM:SS): " + trimmed);
        }
    }

    private void validateLoanInput(LoanInput loanInput) {
        if (loanInput == null) {
            throw new IllegalArgumentException("loan is required");
        }
        if (loanInput.getBorrowerId() == null) {
            throw new IllegalArgumentException("borrowerId is required");
        }
        if (loanInput.getInstitutionId() == null) {
            throw new IllegalArgumentException("institutionId is required");
        }
        if (loanInput.getLoanAmount() == null || loanInput.getLoanAmount() <= 0) {
            throw new IllegalArgumentException("loanAmount must be greater than 0");
        }
        if (loanInput.getInterestRate() == null || loanInput.getInterestRate() < 0) {
            throw new IllegalArgumentException("interestRate must be 0 or greater");
        }
        if (loanInput.getTenureMonths() == null || loanInput.getTenureMonths() <= 0) {
            throw new IllegalArgumentException("tenureMonths must be greater than 0");
        }
        if (loanInput.getStatus() == null || loanInput.getStatus().isBlank()) {
            throw new IllegalArgumentException("status is required");
        }
    }

    // Input class
    public static class LoanInput {
        private Long borrowerId;
        private Long institutionId;
        private Double loanAmount;
        private Double interestRate;
        private Integer tenureMonths;
        private String disbursementDate;
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

        public String getDisbursementDate() {
            return disbursementDate;
        }

        public void setDisbursementDate(String disbursementDate) {
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
package com.riskmanagement.controller;

import com.riskmanagement.model.Loan;
import com.riskmanagement.model.User;
import com.riskmanagement.service.BorrowerService;
import com.riskmanagement.service.LoanService;
import com.riskmanagement.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Controller
public class LoanGraphQLController {

    private static final Logger log = LoggerFactory.getLogger(LoanGraphQLController.class);

    @Autowired
    private LoanService loanService;

    @Autowired
    private BorrowerService borrowerService;

    @Autowired
    private UserService userService;

    @QueryMapping
    public List<Loan> loans() {
        return loanService.getAllLoans();
    }

    @QueryMapping
    public Optional<Loan> loan(@Argument("id") Long id) {
        return loanService.getLoanById(id);
    }

    @MutationMapping
    public Loan createLoan(@Argument("loan") LoanInput loanInput, Authentication authentication) {
        log.info("[GraphQL createLoan] borrowerId={}, amount={}, tenure={}",
                loanInput.getBorrowerId(), loanInput.getLoanAmount(), loanInput.getTenureMonths());
        User user = getAuthenticatedUser(authentication);
        validateBorrowerOwnership(loanInput.getBorrowerId(), user);

        Loan loan = new Loan();
        loan.setBorrowerId(loanInput.getBorrowerId());
        loan.setInstitutionId(user.getInstitutionId());
        loan.setLoanAmount(loanInput.getLoanAmount());
        loan.setInterestRate(loanInput.getInterestRate());
        loan.setTenureMonths(loanInput.getTenureMonths());
        loan.setDisbursementDate(loanInput.getDisbursementDate());
        loan.setStatus(loanInput.getStatus());
        loan.setCreatedAt(LocalDateTime.now());
        Loan saved = loanService.saveLoan(loan);
        log.info("[GraphQL createLoan] Saved loanId={}", saved.getLoanId());
        return saved;
    }

    @MutationMapping
    public Loan updateLoan(@Argument("id") Long id, @Argument("loan") LoanInput loanInput, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        validateBorrowerOwnership(loanInput.getBorrowerId(), user);

        Loan loan = loanService.getLoanById(id)
                .orElseThrow(() -> new RuntimeException("Loan not found with id: " + id));
        loan.setBorrowerId(loanInput.getBorrowerId());
        loan.setInstitutionId(user.getInstitutionId());
        loan.setLoanAmount(loanInput.getLoanAmount());
        loan.setInterestRate(loanInput.getInterestRate());
        loan.setTenureMonths(loanInput.getTenureMonths());
        loan.setDisbursementDate(loanInput.getDisbursementDate());
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

    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("Authentication is required");
        }

        Long userId;
        try {
            userId = Long.parseLong(authentication.getName());
        } catch (NumberFormatException ex) {
            throw new RuntimeException("Invalid authenticated user");
        }

        return userService.getUserById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private void validateBorrowerOwnership(Long borrowerId, User user) {
        if (borrowerId == null || !borrowerService.borrowerBelongsToUser(borrowerId, user.getUserId(), user.getInstitutionId())) {
            throw new RuntimeException("Borrower is not accessible for this user");
        }
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
package com.riskmanagement.controller;

import com.riskmanagement.model.Loan;
import com.riskmanagement.model.User;
import com.riskmanagement.service.BorrowerService;
import com.riskmanagement.service.LoanService;
import com.riskmanagement.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private static final Logger log = LoggerFactory.getLogger(LoanController.class);

    @Autowired
    private LoanService loanService;

    @Autowired
    private BorrowerService borrowerService;

    @Autowired
    private UserService userService;

    @GetMapping
    public List<Loan> getAllLoans() {
        return loanService.getAllLoans();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Loan> getLoanById(@PathVariable Long id) {
        return loanService.getLoanById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Loan createLoan(@RequestBody Loan loan, Authentication authentication) {
        log.info("[POST /api/loans] REST createLoan borrowerId={}, amount={}, tenure={}",
                loan.getBorrowerId(), loan.getLoanAmount(), loan.getTenureMonths());
        User user = getAuthenticatedUser(authentication);
        if (loan.getBorrowerId() == null || !borrowerService.borrowerBelongsToUser(loan.getBorrowerId(), user.getUserId(), user.getInstitutionId())) {
            log.warn("[POST /api/loans] FORBIDDEN — borrowerId={} not owned by userId={}",
                    loan.getBorrowerId(), user.getUserId());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Borrower is not accessible for this user");
        }

        loan.setInstitutionId(user.getInstitutionId());
        if (loan.getCreatedAt() == null) {
            loan.setCreatedAt(LocalDateTime.now());
        }
        Loan saved = loanService.saveLoan(loan);
        log.info("[POST /api/loans] Saved loanId={}", saved.getLoanId());
        return saved;
    }

    @PutMapping("/{id}")
    public ResponseEntity<Loan> updateLoan(@PathVariable Long id, @RequestBody Loan loan) {
        if (!loanService.getLoanById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        loan.setLoanId(id);
        return ResponseEntity.ok(loanService.saveLoan(loan));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLoan(@PathVariable Long id) {
        if (!loanService.getLoanById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        loanService.deleteLoan(id);
        return ResponseEntity.noContent().build();
    }

    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }

        Long userId;
        try {
            userId = Long.parseLong(authentication.getName());
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authenticated user");
        }

        return userService.getUserById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}
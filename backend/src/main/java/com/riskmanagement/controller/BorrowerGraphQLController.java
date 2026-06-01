package com.riskmanagement.controller;

import com.riskmanagement.model.Borrower;
import com.riskmanagement.service.BorrowerService;
import com.riskmanagement.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Optional;

@Controller
public class BorrowerGraphQLController {
    @Autowired
    private BorrowerService borrowerService;
    @Autowired
    private UserService userService;

    @QueryMapping
    public List<Borrower> borrowers() {
        return borrowerService.getAllBorrowers();
    }

    @QueryMapping
    public Borrower borrower(@Argument Long id) {
        return borrowerService.getBorrowerById(id).orElse(null);
    }

    @QueryMapping
    public List<Borrower> searchBorrowers(@Argument String query) {
        return borrowerService.searchBorrowers(query);
    }

    @QueryMapping
    public BorrowerLookups borrowerLookups() {
        BorrowerLookups lookups = new BorrowerLookups();
        lookups.sectors = borrowerService.getDistinctBusinessSectors();
        lookups.locations = borrowerService.getDistinctLocations();
        return lookups;
    }

    @QueryMapping
    public Borrower myBorrowerProfile(Authentication authentication) {
        var user = getAuthenticatedUser(authentication);
        return borrowerService.getLatestBorrowerProfile(user.getUserId(), user.getInstitutionId()).orElse(null);
    }

    @MutationMapping
    public Borrower createBorrower(@Argument BorrowerInput borrowerInput, Authentication authentication) {
        var user = getAuthenticatedUser(authentication);
        Borrower borrower = toBorrower(borrowerInput);
        borrower.setUserId(user.getUserId());
        borrower.setInstitutionId(user.getInstitutionId());
        return borrowerService.saveBorrower(borrower);
    }

    @MutationMapping
    public Borrower updateBorrower(@Argument Long id, @Argument BorrowerInput borrowerInput) {
        Optional<Borrower> existing = borrowerService.getBorrowerById(id);
        if (existing.isEmpty()) return null;
        Borrower borrower = toBorrower(borrowerInput);
        borrower.setBorrowerId(id);
        return borrowerService.saveBorrower(borrower);
    }

    @MutationMapping
    public Boolean deleteBorrower(@Argument Long id) {
        if (borrowerService.getBorrowerById(id).isEmpty()) return false;
        borrowerService.deleteBorrower(id);
        return true;
    }

    @MutationMapping
    public Borrower updateCreditScore(@Argument Long id, @Argument Integer creditScore) {
        return borrowerService.updateCreditScore(id, creditScore).orElse(null);
    }

    // --- Helper classes and methods ---
    public static class BorrowerLookups {
        public List<String> sectors;
        public List<String> locations;
    }

    public static class BorrowerInput {
        public Long userId;
        public Long institutionId;
        public String fullName;
        public String nationalId;
        public String gender;
        public Integer age;
        public String location;
        public String businessSector;
        public Double monthlyIncome;
        public Double collateralValue;
        public Integer creditScore;
    }

    private Borrower toBorrower(BorrowerInput input) {
        Borrower b = new Borrower();
        b.setUserId(input.userId);
        b.setInstitutionId(input.institutionId);
        b.setFullName(input.fullName);
        b.setNationalId(input.nationalId);
        b.setGender(input.gender);
        b.setAge(input.age);
        b.setLocation(input.location);
        b.setBusinessSector(input.businessSector);
        b.setMonthlyIncome(input.monthlyIncome);
        b.setCollateralValue(input.collateralValue);
        b.setCreditScore(input.creditScore);
        return b;
    }

    private com.riskmanagement.model.User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("Authentication is required");
        }
        Long userId = Long.parseLong(authentication.getName());
        return userService.getUserById(userId).orElseThrow(() -> new RuntimeException("User not found"));
    }
}

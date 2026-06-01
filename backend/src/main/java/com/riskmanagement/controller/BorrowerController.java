package com.riskmanagement.controller;

import com.riskmanagement.model.Borrower;
import com.riskmanagement.model.User;
import com.riskmanagement.service.BorrowerService;
import com.riskmanagement.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/borrowers")
public class BorrowerController {
    @GetMapping("/search")
    public List<Borrower> searchBorrowers(@RequestParam("q") String query) {
        return borrowerService.searchBorrowers(query);
    }

    @PutMapping("/{id}/credit-score")
    public ResponseEntity<Borrower> updateCreditScore(@PathVariable Long id, @RequestBody Map<String, Integer> payload, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        // Only allow Admin role
        if (!"Admin".equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only Admin can update credit score");
        }
        Integer creditScore = payload.get("creditScore");
        if (creditScore == null || creditScore < 300 || creditScore > 850) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Credit score must be between 300 and 850");
        }
        return borrowerService.updateCreditScore(id, creditScore)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Autowired
    private BorrowerService borrowerService;

    @Autowired
    private UserService userService;

    @GetMapping
    public List<Borrower> getAllBorrowers() {
        return borrowerService.getAllBorrowers();
    }

    @GetMapping("/lookups")
    public Map<String, List<String>> getBorrowerLookups() {
        Map<String, List<String>> lookups = new HashMap<>();
        lookups.put("sectors", borrowerService.getDistinctBusinessSectors());
        lookups.put("locations", borrowerService.getDistinctLocations());
        return lookups;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Borrower> getBorrowerById(@PathVariable Long id) {
        return borrowerService.getBorrowerById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/me/profile")
    public ResponseEntity<BorrowerProfileResponse> getMyBorrowerProfile(Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        return borrowerService.getLatestBorrowerProfile(user.getUserId(), user.getInstitutionId())
                .map(BorrowerProfileResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Borrower createBorrower(@RequestBody Borrower borrower, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        borrower.setUserId(user.getUserId());
        borrower.setInstitutionId(user.getInstitutionId());
        return borrowerService.saveBorrower(borrower);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Borrower> updateBorrower(@PathVariable Long id, @RequestBody Borrower borrower) {
        if (!borrowerService.getBorrowerById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        borrower.setBorrowerId(id);
        return ResponseEntity.ok(borrowerService.saveBorrower(borrower));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBorrower(@PathVariable Long id) {
        if (!borrowerService.getBorrowerById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        borrowerService.deleteBorrower(id);
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

    public static class BorrowerProfileResponse {
        private Long borrowerId;
        private String fullName;
        private String nationalId;
        private String gender;
        private Integer age;
        private String location;
        private String businessSector;
        private Double monthlyIncome;
        private Double collateralValue;

        public static BorrowerProfileResponse from(Borrower borrower) {
            BorrowerProfileResponse response = new BorrowerProfileResponse();
            response.setBorrowerId(borrower.getBorrowerId());
            response.setFullName(borrower.getFullName());
            response.setNationalId(borrower.getNationalId());
            response.setGender(borrower.getGender());
            response.setAge(borrower.getAge());
            response.setLocation(borrower.getLocation());
            response.setBusinessSector(borrower.getBusinessSector());
            response.setMonthlyIncome(borrower.getMonthlyIncome());
            response.setCollateralValue(borrower.getCollateralValue());
            return response;
        }

        public Long getBorrowerId() {
            return borrowerId;
        }

        public void setBorrowerId(Long borrowerId) {
            this.borrowerId = borrowerId;
        }

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public String getNationalId() {
            return nationalId;
        }

        public void setNationalId(String nationalId) {
            this.nationalId = nationalId;
        }

        public String getGender() {
            return gender;
        }

        public void setGender(String gender) {
            this.gender = gender;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }

        public String getBusinessSector() {
            return businessSector;
        }

        public void setBusinessSector(String businessSector) {
            this.businessSector = businessSector;
        }

        public Double getMonthlyIncome() {
            return monthlyIncome;
        }

        public void setMonthlyIncome(Double monthlyIncome) {
            this.monthlyIncome = monthlyIncome;
        }

        public Double getCollateralValue() {
            return collateralValue;
        }

        public void setCollateralValue(Double collateralValue) {
            this.collateralValue = collateralValue;
        }
    }
}
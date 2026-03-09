package com.riskmanagement.controller;

import com.riskmanagement.model.Borrower;
import com.riskmanagement.model.Loan;
import com.riskmanagement.model.RiskScore;
import com.riskmanagement.service.RiskCalculationService;
import com.riskmanagement.service.RiskScoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/risk-scores")
public class RiskScoreController {

    @Autowired
    private RiskScoreService riskScoreService;

    @Autowired
    private RiskCalculationService riskCalculationService;

    @GetMapping
    public List<RiskScore> getAllRiskScores() {
        return riskScoreService.getAllRiskScores();
    }

    @GetMapping("/{id}")
    public ResponseEntity<RiskScore> getRiskScoreById(@PathVariable Long id) {
        return riskScoreService.getRiskScoreById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public RiskScore createRiskScore(@RequestBody RiskScore riskScore) {
        return riskScoreService.saveRiskScore(riskScore);
    }

    @PostMapping("/calculate")
    public ResponseEntity<RiskScore> calculateRiskScore(@RequestBody CalculateRiskRequest request) {
        Borrower borrower = new Borrower();
        borrower.setFullName(request.getFullName());
        borrower.setNationalId(request.getNationalId());
        borrower.setGender(request.getGender());
        borrower.setAge(request.getAge());
        borrower.setLocation(request.getLocation());
        borrower.setBusinessSector(request.getBusinessSector());
        borrower.setMonthlyIncome(request.getMonthlyIncome());
        borrower.setCollateralValue(request.getCollateralValue());

        Loan loan = new Loan();
        loan.setLoanAmount(request.getLoanAmount());
        loan.setInterestRate(request.getInterestRate());
        loan.setTenureMonths(request.getTenureMonths());
        loan.setStatus(request.getStatus());

        RiskScore result = riskCalculationService.calculateRiskScore(loan, borrower);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RiskScore> updateRiskScore(@PathVariable Long id, @RequestBody RiskScore riskScore) {
        if (!riskScoreService.getRiskScoreById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        riskScore.setRiskId(id);
        return ResponseEntity.ok(riskScoreService.saveRiskScore(riskScore));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRiskScore(@PathVariable Long id) {
        if (!riskScoreService.getRiskScoreById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        riskScoreService.deleteRiskScore(id);
        return ResponseEntity.noContent().build();
    }

    public static class CalculateRiskRequest {
        private String fullName;
        private String nationalId;
        private String gender;
        private Integer age;
        private String location;
        private String businessSector;
        private Double monthlyIncome;
        private Double collateralValue;
        private Double loanAmount;
        private Double interestRate;
        private Integer tenureMonths;
        private String status;

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getNationalId() { return nationalId; }
        public void setNationalId(String nationalId) { this.nationalId = nationalId; }
        public String getGender() { return gender; }
        public void setGender(String gender) { this.gender = gender; }
        public Integer getAge() { return age; }
        public void setAge(Integer age) { this.age = age; }
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
        public String getBusinessSector() { return businessSector; }
        public void setBusinessSector(String businessSector) { this.businessSector = businessSector; }
        public Double getMonthlyIncome() { return monthlyIncome; }
        public void setMonthlyIncome(Double monthlyIncome) { this.monthlyIncome = monthlyIncome; }
        public Double getCollateralValue() { return collateralValue; }
        public void setCollateralValue(Double collateralValue) { this.collateralValue = collateralValue; }
        public Double getLoanAmount() { return loanAmount; }
        public void setLoanAmount(Double loanAmount) { this.loanAmount = loanAmount; }
        public Double getInterestRate() { return interestRate; }
        public void setInterestRate(Double interestRate) { this.interestRate = interestRate; }
        public Integer getTenureMonths() { return tenureMonths; }
        public void setTenureMonths(Integer tenureMonths) { this.tenureMonths = tenureMonths; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}

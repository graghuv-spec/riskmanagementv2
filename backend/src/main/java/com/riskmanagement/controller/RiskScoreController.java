package com.riskmanagement.controller;

import com.riskmanagement.model.Borrower;
import com.riskmanagement.model.Loan;
import com.riskmanagement.model.RiskScore;
import com.riskmanagement.service.RiskCalculationService;
import com.riskmanagement.service.RiskScoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/risk-scores")
public class RiskScoreController {

    private static final Logger log = LoggerFactory.getLogger(RiskScoreController.class);

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
        log.info("[GET /api/risk-scores/{}] Fetching risk score", id);
        return riskScoreService.getRiskScoreById(id)
                .map(score -> {
                    log.info("[GET /api/risk-scores/{}] Found: score={}, grade={}",
                            id, score.getRiskScore(), score.getRiskGrade());
                    return ResponseEntity.ok(score);
                })
                .orElseGet(() -> {
                    log.warn("[GET /api/risk-scores/{}] Not found", id);
                    return ResponseEntity.notFound().build();
                });
    }

    @PostMapping
    public RiskScore createRiskScore(@RequestBody RiskScore riskScore) {
        log.info("[POST /api/risk-scores] Saving risk score for loanId={}", riskScore.getLoanId());
        RiskScore saved = riskScoreService.saveRiskScore(riskScore);
        log.info("[POST /api/risk-scores] Saved riskId={}, riskScore={}, riskGrade={}",
                saved.getRiskId(), saved.getRiskScore(), saved.getRiskGrade());
        return saved;
    }

    @PostMapping("/calculate")
    public ResponseEntity<RiskScore> calculateRiskScore(@RequestBody CalculateRiskRequest request) {
        log.info("[POST /api/risk-scores/calculate] name={}, sector={}, loanAmount={}, income={}",
                request.getFullName(), request.getBusinessSector(),
                request.getLoanAmount(), request.getMonthlyIncome());
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
        log.info("[POST /api/risk-scores/calculate] Result: score={}, grade={}, pd={}, limit={}",
                result.getRiskScore(), result.getRiskGrade(),
                result.getProbabilityDefault(), result.getRecommendedLimit());
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

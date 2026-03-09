package com.riskmanagement.service;

import com.riskmanagement.model.Borrower;
import com.riskmanagement.model.Loan;
import com.riskmanagement.model.Repayment;
import com.riskmanagement.model.RiskScore;
import com.riskmanagement.repository.RepaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RiskCalculationService {

    @Autowired
    private RepaymentRepository repaymentRepository;

    // Weights
    private static final double INCOME_STABILITY_WEIGHT = 0.3;
    private static final double REPAYMENT_HISTORY_WEIGHT = 0.3;
    private static final double COLLATERAL_RATIO_WEIGHT = 0.2;
    private static final double SECTOR_RISK_WEIGHT = 0.1;
    private static final double LOCATION_RISK_WEIGHT = 0.1;

    public RiskScore calculateRiskScore(Loan loan, Borrower borrower) {
        // Calculate individual scores (simplified)
        double incomeScore = calculateIncomeScore(borrower.getMonthlyIncome());
        double repaymentScore = calculateRepaymentScore(loan.getLoanId());
        double collateralScore = calculateCollateralScore(borrower.getCollateralValue(), loan.getLoanAmount());
        double sectorRiskScore = calculateSectorRiskScore(borrower.getBusinessSector());
        double regionalScore = calculateRegionalScore(borrower.getLocation());

        // Calculate risk score
        double riskScore = (INCOME_STABILITY_WEIGHT * incomeScore) +
                           (REPAYMENT_HISTORY_WEIGHT * repaymentScore) +
                           (COLLATERAL_RATIO_WEIGHT * collateralScore) +
                           (SECTOR_RISK_WEIGHT * sectorRiskScore) +
                           (LOCATION_RISK_WEIGHT * regionalScore);

        // Scale to 0-100
        riskScore = Math.min(100, Math.max(0, riskScore));

        // Probability of default (simplified)
        double pd = 1 / (1 + Math.exp(- (riskScore / 10 - 5))); // Logistic

        // Risk grade
        String riskGrade = getRiskGrade(riskScore);

        // Recommended limit (simplified)
        double recommendedLimit = borrower.getMonthlyIncome() * 0.5 * (riskScore / 100);

        RiskScore riskScoreEntity = new RiskScore();
        riskScoreEntity.setLoanId(loan.getLoanId());
        riskScoreEntity.setRiskScore(riskScore);
        riskScoreEntity.setProbabilityDefault(pd);
        riskScoreEntity.setRiskGrade(riskGrade);
        riskScoreEntity.setRecommendedLimit(recommendedLimit);
        riskScoreEntity.setModelVersion("Rule-Based v1");
        riskScoreEntity.setExplanationJson("{\"method\": \"rule-based\"}");
        riskScoreEntity.setCreatedAt(LocalDateTime.now());

        return riskScoreEntity;
    }

    private double calculateIncomeScore(double monthlyIncome) {
        // Simplified: higher income better score
        if (monthlyIncome > 5000) return 100;
        if (monthlyIncome > 2000) return 80;
        if (monthlyIncome > 1000) return 60;
        return 40;
    }

    private double calculateRepaymentScore(Long loanId) {
        List<Repayment> repayments = repaymentRepository.findAll(); // Simplified, should filter by loanId
        // Simplified: if no past due, 100
        long pastDue = repayments.stream().filter(r -> r.getDaysPastDue() > 0).count();
        return pastDue == 0 ? 100 : 50;
    }

    private double calculateCollateralScore(double collateralValue, double loanAmount) {
        double ratio = collateralValue / loanAmount;
        if (ratio > 2) return 100;
        if (ratio > 1) return 80;
        if (ratio > 0.5) return 60;
        return 40;
    }

    private double calculateSectorRiskScore(String sector) {
        // Simplified: some sectors riskier
        switch (sector.toLowerCase()) {
            case "technology": return 90;
            case "finance": return 80;
            case "agriculture": return 60;
            default: return 70;
        }
    }

    private double calculateRegionalScore(String location) {
        // Simplified
        return 75; // Average
    }

    private String getRiskGrade(double score) {
        if (score >= 80) return "A";
        if (score >= 60) return "B";
        if (score >= 40) return "C";
        return "D";
    }
}
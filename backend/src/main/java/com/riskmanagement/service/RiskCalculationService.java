package com.riskmanagement.service;

import com.riskmanagement.config.AppProperties;
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

    private final RepaymentRepository repaymentRepository;
    private final AppProperties appProperties;

    @Autowired
    public RiskCalculationService(RepaymentRepository repaymentRepository, AppProperties appProperties) {
        this.repaymentRepository = repaymentRepository;
        this.appProperties = appProperties;
    }

    public RiskScore calculateRiskScore(Loan loan, Borrower borrower) {
        AppProperties.Risk risk = appProperties.getRisk();
        double maxScore = risk.getMaxScore();
        double minScore = risk.getMinScore();

        // Calculate individual scores (simplified)
        double incomeScore = calculateIncomeScore(borrower.getMonthlyIncome());
        double repaymentScore = calculateRepaymentScore(loan.getLoanId());
        double collateralScore = calculateCollateralScore(borrower.getCollateralValue(), loan.getLoanAmount());
        double sectorRiskScore = calculateSectorRiskScore(borrower.getBusinessSector());
        double regionalScore = calculateRegionalScore(borrower.getLocation());

        // Calculate risk score
        double riskScore = (risk.getIncomeStabilityWeight() * incomeScore) +
                           (risk.getRepaymentHistoryWeight() * repaymentScore) +
                           (risk.getCollateralRatioWeight() * collateralScore) +
                           (risk.getSectorRiskWeight() * sectorRiskScore) +
                           (risk.getLocationRiskWeight() * regionalScore);

        // Scale to 0-100
        riskScore = Math.min(maxScore, Math.max(minScore, riskScore));

        // Probability of default (simplified)
        double pd = 1 / (1 + Math.exp(- (riskScore / risk.getPdDivisor() - risk.getPdOffset()))); // Logistic

        // Risk grade
        String riskGrade = getRiskGrade(riskScore);

        // Recommended limit (simplified)
        double recommendedLimit = borrower.getMonthlyIncome()
                * risk.getRecommendedLimitIncomeMultiplier()
            * (riskScore / maxScore);

        RiskScore riskScoreEntity = new RiskScore();
        riskScoreEntity.setLoanId(loan.getLoanId());
        riskScoreEntity.setRiskScore(riskScore);
        riskScoreEntity.setProbabilityDefault(pd);
        riskScoreEntity.setRiskGrade(riskGrade);
        riskScoreEntity.setRecommendedLimit(recommendedLimit);
        riskScoreEntity.setModelVersion(risk.getModelVersion());
        riskScoreEntity.setExplanationJson(risk.getExplanationJson());
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
        List<Repayment> repayments = repaymentRepository.findByLoanId(loanId);
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
        AppProperties.Risk risk = appProperties.getRisk();
        if (sector == null) {
            return risk.getSectorDefaultScore();
        }

        // Simplified: some sectors are riskier.
        switch (sector.toLowerCase()) {
            case "technology": return risk.getSectorTechnologyScore();
            case "finance": return risk.getSectorFinanceScore();
            case "agriculture": return risk.getSectorAgricultureScore();
            default: return risk.getSectorDefaultScore();
        }
    }

    private double calculateRegionalScore(String location) {
        // Simplified: use configurable average regional score.
        return appProperties.getRisk().getDefaultRegionalScore();
    }

    private String getRiskGrade(double score) {
        AppProperties.Risk risk = appProperties.getRisk();

        if (score >= risk.getGradeAThreshold()) return "A";
        if (score >= risk.getGradeBThreshold()) return "B";
        if (score >= risk.getGradeCThreshold()) return "C";
        return "D";
    }
}
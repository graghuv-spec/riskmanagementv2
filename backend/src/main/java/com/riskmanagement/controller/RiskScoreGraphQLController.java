package com.riskmanagement.controller;

import com.riskmanagement.model.RiskScore;
import com.riskmanagement.service.RiskScoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Optional;

@Controller
public class RiskScoreGraphQLController {
    @Autowired
    private RiskScoreService riskScoreService;

    @QueryMapping
    public List<RiskScore> riskScores() {
        return riskScoreService.getAllRiskScores();
    }

    @QueryMapping
    public RiskScore riskScore(@Argument Long id) {
        return riskScoreService.getRiskScoreById(id).orElse(null);
    }

    @QueryMapping
    public List<RiskScore> riskScoresByLoan(@Argument Long loanId) {
        return riskScoreService.getRiskScoresByLoanId(loanId);
    }

    @MutationMapping
    public RiskScore createRiskScore(@Argument RiskScoreInput riskScore) {
        RiskScore r = toRiskScore(riskScore);
        return riskScoreService.saveRiskScore(r);
    }

    @MutationMapping
    public RiskScore updateRiskScore(@Argument Long id, @Argument RiskScoreInput riskScore) {
        Optional<RiskScore> existing = riskScoreService.getRiskScoreById(id);
        if (existing.isEmpty()) return null;
        RiskScore r = toRiskScore(riskScore);
        r.setRiskId(id);
        return riskScoreService.saveRiskScore(r);
    }

    @MutationMapping
    public Boolean deleteRiskScore(@Argument Long id) {
        if (riskScoreService.getRiskScoreById(id).isEmpty()) return false;
        riskScoreService.deleteRiskScore(id);
        return true;
    }

    public static class RiskScoreInput {
        public Long loanId;
        public Double riskScore;
        public Double probabilityDefault;
        public String riskGrade;
        public Double recommendedLimit;
        public String modelVersion;
        public String explanationJson;
    }

    private RiskScore toRiskScore(RiskScoreInput input) {
        RiskScore r = new RiskScore();
        r.setLoanId(input.loanId);
        r.setRiskScore(input.riskScore);
        r.setProbabilityDefault(input.probabilityDefault);
        r.setRiskGrade(input.riskGrade);
        r.setRecommendedLimit(input.recommendedLimit);
        r.setModelVersion(input.modelVersion);
        r.setExplanationJson(input.explanationJson);
        return r;
    }
}

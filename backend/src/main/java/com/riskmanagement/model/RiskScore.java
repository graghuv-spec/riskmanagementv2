package com.riskmanagement.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "risk_scores")
public class RiskScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long riskId;

    private Long loanId;

    private Double riskScore; // 0-100

    private Double probabilityDefault;

    private String riskGrade; // A/B/C/D

    private Double recommendedLimit;

    private String modelVersion;

    private String explanationJson;

    private LocalDateTime createdAt;

    // Getters and Setters
    public Long getRiskId() {
        return riskId;
    }

    public void setRiskId(Long riskId) {
        this.riskId = riskId;
    }

    public Long getLoanId() {
        return loanId;
    }

    public void setLoanId(Long loanId) {
        this.loanId = loanId;
    }

    public Double getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Double riskScore) {
        this.riskScore = riskScore;
    }

    public Double getProbabilityDefault() {
        return probabilityDefault;
    }

    public void setProbabilityDefault(Double probabilityDefault) {
        this.probabilityDefault = probabilityDefault;
    }

    public String getRiskGrade() {
        return riskGrade;
    }

    public void setRiskGrade(String riskGrade) {
        this.riskGrade = riskGrade;
    }

    public Double getRecommendedLimit() {
        return recommendedLimit;
    }

    public void setRecommendedLimit(Double recommendedLimit) {
        this.recommendedLimit = recommendedLimit;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public String getExplanationJson() {
        return explanationJson;
    }

    public void setExplanationJson(String explanationJson) {
        this.explanationJson = explanationJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
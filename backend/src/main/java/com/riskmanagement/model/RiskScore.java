package com.riskmanagement.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "risk_scores")
public class RiskScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long riskId;

    @NotNull(message = "loanId is required")
    private Long loanId;

    @NotNull(message = "riskScore is required")
    @DecimalMin(value = "0.0", message = "riskScore must be at least 0")
    @DecimalMax(value = "100.0", message = "riskScore must be at most 100")
    private Double riskScore; // 0-100

    @NotNull(message = "probabilityDefault is required")
    @DecimalMin(value = "0.0", message = "probabilityDefault must be at least 0")
    @DecimalMax(value = "1.0", message = "probabilityDefault must be at most 1")
    private Double probabilityDefault;

    @NotBlank(message = "riskGrade is required")
    private String riskGrade; // A/B/C/D

    @NotNull(message = "recommendedLimit is required")
    @DecimalMin(value = "0.0", message = "recommendedLimit must be 0 or greater")
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
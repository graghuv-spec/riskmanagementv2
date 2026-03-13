package com.riskmanagement.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "portfolio_metrics")
public class PortfolioMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long metricId;

    @NotNull(message = "institutionId is required")
    private Long institutionId;

    @NotNull(message = "par30 is required")
    @DecimalMin(value = "0.0", message = "par30 must be 0 or greater")
    private Double par30;

    @NotNull(message = "par90 is required")
    @DecimalMin(value = "0.0", message = "par90 must be 0 or greater")
    private Double par90;

    @NotNull(message = "sectorConcentration is required")
    @DecimalMin(value = "0.0", message = "sectorConcentration must be 0 or greater")
    private Double sectorConcentration;

    @NotNull(message = "regionRiskIndex is required")
    @DecimalMin(value = "0.0", message = "regionRiskIndex must be 0 or greater")
    private Double regionRiskIndex;

    @NotNull(message = "forecastDefaultRate is required")
    @DecimalMin(value = "0.0", message = "forecastDefaultRate must be 0 or greater")
    private Double forecastDefaultRate;

    private LocalDateTime generatedAt;

    // Getters and Setters
    public Long getMetricId() {
        return metricId;
    }

    public void setMetricId(Long metricId) {
        this.metricId = metricId;
    }

    public Long getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(Long institutionId) {
        this.institutionId = institutionId;
    }

    public Double getPar30() {
        return par30;
    }

    public void setPar30(Double par30) {
        this.par30 = par30;
    }

    public Double getPar90() {
        return par90;
    }

    public void setPar90(Double par90) {
        this.par90 = par90;
    }

    public Double getSectorConcentration() {
        return sectorConcentration;
    }

    public void setSectorConcentration(Double sectorConcentration) {
        this.sectorConcentration = sectorConcentration;
    }

    public Double getRegionRiskIndex() {
        return regionRiskIndex;
    }

    public void setRegionRiskIndex(Double regionRiskIndex) {
        this.regionRiskIndex = regionRiskIndex;
    }

    public Double getForecastDefaultRate() {
        return forecastDefaultRate;
    }

    public void setForecastDefaultRate(Double forecastDefaultRate) {
        this.forecastDefaultRate = forecastDefaultRate;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }
}
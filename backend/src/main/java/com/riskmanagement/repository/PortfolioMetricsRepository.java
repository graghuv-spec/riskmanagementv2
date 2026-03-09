package com.riskmanagement.repository;

import com.riskmanagement.model.PortfolioMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PortfolioMetricsRepository extends JpaRepository<PortfolioMetrics, Long> {
}
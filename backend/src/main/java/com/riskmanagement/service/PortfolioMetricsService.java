package com.riskmanagement.service;

import com.riskmanagement.model.PortfolioMetrics;
import com.riskmanagement.repository.PortfolioMetricsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PortfolioMetricsService {

    @Autowired
    private PortfolioMetricsRepository portfolioMetricsRepository;

    public List<PortfolioMetrics> getAllPortfolioMetrics() {
        return portfolioMetricsRepository.findAll();
    }

    public Optional<PortfolioMetrics> getPortfolioMetricsById(Long id) {
        return portfolioMetricsRepository.findById(id);
    }

    public PortfolioMetrics savePortfolioMetrics(PortfolioMetrics portfolioMetrics) {
        return portfolioMetricsRepository.save(portfolioMetrics);
    }

    public void deletePortfolioMetrics(Long id) {
        portfolioMetricsRepository.deleteById(id);
    }
}
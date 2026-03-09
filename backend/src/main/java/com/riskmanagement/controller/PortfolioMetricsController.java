package com.riskmanagement.controller;

import com.riskmanagement.model.PortfolioMetrics;
import com.riskmanagement.service.PortfolioMetricsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/portfolio-metrics")
public class PortfolioMetricsController {

    @Autowired
    private PortfolioMetricsService portfolioMetricsService;

    @GetMapping
    public List<PortfolioMetrics> getAll() {
        return portfolioMetricsService.getAllPortfolioMetrics();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PortfolioMetrics> getById(@PathVariable Long id) {
        return portfolioMetricsService.getPortfolioMetricsById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public PortfolioMetrics create(@RequestBody PortfolioMetrics m) {
        return portfolioMetricsService.savePortfolioMetrics(m);
    }
}

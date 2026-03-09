package com.riskmanagement.controller;

import com.riskmanagement.model.RiskScore;
import com.riskmanagement.service.RiskScoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/risk-scores")
public class RiskScoreController {

    @Autowired
    private RiskScoreService riskScoreService;

    @GetMapping
    public List<RiskScore> getAllRiskScores() {
        return riskScoreService.getAllRiskScores();
    }

    @GetMapping("/{id}")
    public ResponseEntity<RiskScore> getRiskScoreById(@PathVariable Long id) {
        return riskScoreService.getRiskScoreById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public RiskScore createRiskScore(@RequestBody RiskScore riskScore) {
        return riskScoreService.saveRiskScore(riskScore);
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
}
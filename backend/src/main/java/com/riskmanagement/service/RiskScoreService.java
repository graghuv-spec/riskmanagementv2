package com.riskmanagement.service;

import com.riskmanagement.model.RiskScore;
import com.riskmanagement.repository.RiskScoreRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RiskScoreService {

    @Autowired
    private RiskScoreRepository riskScoreRepository;

    public List<RiskScore> getAllRiskScores() {
        return riskScoreRepository.findAll();
    }

    public Optional<RiskScore> getRiskScoreById(Long id) {
        return riskScoreRepository.findById(id);
    }

    public RiskScore saveRiskScore(RiskScore riskScore) {
        return riskScoreRepository.save(riskScore);
    }

    public void deleteRiskScore(Long id) {
        riskScoreRepository.deleteById(id);
    }
}
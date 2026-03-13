package com.riskmanagement.repository;

import com.riskmanagement.model.RiskScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RiskScoreRepository extends JpaRepository<RiskScore, Long> {
    java.util.Optional<RiskScore> findByLoanId(Long loanId);
}
package com.riskmanagement.service;

import com.riskmanagement.model.Repayment;
import com.riskmanagement.repository.RepaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RepaymentService {

    @Autowired
    private RepaymentRepository repaymentRepository;

    public List<Repayment> getAllRepayments() {
        return repaymentRepository.findAll();
    }

    public Optional<Repayment> getRepaymentById(Long id) {
        return repaymentRepository.findById(id);
    }

    public Repayment saveRepayment(Repayment repayment) {
        return repaymentRepository.save(repayment);
    }

    public void deleteRepayment(Long id) {
        repaymentRepository.deleteById(id);
    }
}
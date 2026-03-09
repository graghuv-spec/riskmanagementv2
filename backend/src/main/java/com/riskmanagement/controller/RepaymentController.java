package com.riskmanagement.controller;

import com.riskmanagement.model.Repayment;
import com.riskmanagement.service.RepaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/repayments")
public class RepaymentController {

    @Autowired
    private RepaymentService repaymentService;

    @GetMapping
    public List<Repayment> getAll() {
        return repaymentService.getAllRepayments();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Repayment> getById(@PathVariable Long id) {
        return repaymentService.getRepaymentById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Repayment create(@RequestBody Repayment r) {
        return repaymentService.saveRepayment(r);
    }
}

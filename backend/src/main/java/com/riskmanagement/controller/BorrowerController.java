package com.riskmanagement.controller;

import com.riskmanagement.model.Borrower;
import com.riskmanagement.service.BorrowerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/borrowers")
public class BorrowerController {

    @Autowired
    private BorrowerService borrowerService;

    @GetMapping
    public List<Borrower> getAllBorrowers() {
        return borrowerService.getAllBorrowers();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Borrower> getBorrowerById(@PathVariable Long id) {
        return borrowerService.getBorrowerById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Borrower createBorrower(@RequestBody Borrower borrower) {
        return borrowerService.saveBorrower(borrower);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Borrower> updateBorrower(@PathVariable Long id, @RequestBody Borrower borrower) {
        if (!borrowerService.getBorrowerById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        borrower.setBorrowerId(id);
        return ResponseEntity.ok(borrowerService.saveBorrower(borrower));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBorrower(@PathVariable Long id) {
        if (!borrowerService.getBorrowerById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        borrowerService.deleteBorrower(id);
        return ResponseEntity.noContent().build();
    }
}
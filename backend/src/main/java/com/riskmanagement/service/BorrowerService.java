package com.riskmanagement.service;

import com.riskmanagement.model.Borrower;
import com.riskmanagement.repository.BorrowerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BorrowerService {
    public List<Borrower> searchBorrowers(String query) {
        return borrowerRepository.findByFullNameContainingIgnoreCaseOrNationalIdContainingIgnoreCase(query, query);
    }

    public Optional<Borrower> updateCreditScore(Long borrowerId, Integer creditScore) {
        Optional<Borrower> borrowerOpt = borrowerRepository.findById(borrowerId);
        if (borrowerOpt.isPresent()) {
            Borrower borrower = borrowerOpt.get();
            borrower.setCreditScore(creditScore);
            borrowerRepository.save(borrower);
        }
        return borrowerOpt;
    }

    @Autowired
    private BorrowerRepository borrowerRepository;

    public List<Borrower> getAllBorrowers() {
        return borrowerRepository.findAll();
    }

    public Optional<Borrower> getBorrowerById(Long id) {
        return borrowerRepository.findById(id);
    }

    public Optional<Borrower> getLatestBorrowerProfile(Long userId, Long institutionId) {
        return borrowerRepository.findTopByUserIdAndInstitutionIdOrderByCreatedAtDesc(userId, institutionId);
    }

    public boolean borrowerBelongsToUser(Long borrowerId, Long userId, Long institutionId) {
        return borrowerRepository.existsByBorrowerIdAndUserIdAndInstitutionId(borrowerId, userId, institutionId);
    }

    public List<String> getDistinctBusinessSectors() {
        return borrowerRepository.findDistinctBusinessSectors();
    }

    public List<String> getDistinctLocations() {
        return borrowerRepository.findDistinctLocations();
    }

    public Borrower saveBorrower(Borrower borrower) {
        return borrowerRepository.save(borrower);
    }

    public void deleteBorrower(Long id) {
        borrowerRepository.deleteById(id);
    }
}
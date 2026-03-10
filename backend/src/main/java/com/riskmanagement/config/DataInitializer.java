package com.riskmanagement.config;

import com.riskmanagement.model.*;
import com.riskmanagement.repository.*;
import com.riskmanagement.service.RiskCalculationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@Profile("!test")   // never seed demo data when running integration tests
public class DataInitializer {

    @Autowired private InstitutionRepository institutionRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private BorrowerRepository borrowerRepository;
    @Autowired private LoanRepository loanRepository;
    @Autowired private RepaymentRepository repaymentRepository;
    @Autowired private RiskScoreRepository riskScoreRepository;
    @Autowired private PortfolioMetricsRepository portfolioMetricsRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private RiskCalculationService riskCalculationService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Transactional
    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        // Guard: both demo user accounts already present means seed is complete.
        if (userRepository.count() == 2) return;

        // Clean up any partial or duplicate data from a prior interrupted seed.
        // This whole method is @Transactional: if interrupted by SIGTERM the
        // deletes AND inserts both roll back, so the next boot retries cleanly.
        auditLogRepository.deleteAll();
        portfolioMetricsRepository.deleteAll();
        riskScoreRepository.deleteAll();
        repaymentRepository.deleteAll();
        loanRepository.deleteAll();
        borrowerRepository.deleteAll();
        userRepository.deleteAll();
        institutionRepository.deleteAll();

        // Institution
        Institution inst = new Institution();
        inst.setName("MicroFinance Corp");
        inst.setLicenseNumber("MFC-2024-001");
        inst.setContactEmail("admin@mfc.com");
        inst.setSubscriptionPlan("Premium");
        inst.setCreatedAt(LocalDateTime.now().minusMonths(12));
        inst = institutionRepository.save(inst);
        Long instId = inst.getInstitutionId();

        // Users
        User admin = new User();
        admin.setName("Alice Admin");
        admin.setEmail("admin@mfb.com");
        admin.setRole("Admin");
        admin.setPasswordHash(passwordEncoder.encode("password123"));
        admin.setMfaEnabled(true);
        admin.setInstitutionId(instId);
        admin.setCreatedAt(LocalDateTime.now().minusMonths(11));
        userRepository.save(admin);

        User officer = new User();
        officer.setName("Bob Loan Officer");
        officer.setEmail("loan.officer@mfb.com");
        officer.setRole("LoanOfficer");
        officer.setPasswordHash(passwordEncoder.encode("password123"));
        officer.setMfaEnabled(false);
        officer.setInstitutionId(instId);
        officer.setCreatedAt(LocalDateTime.now().minusMonths(10));
        userRepository.save(officer);

        // Borrowers
        String[][] borrowerData = {
            {"Samuel Omondi",   "NID001", "Male",   "32", "Nairobi",      "Technology",    "5500",  "25000"},
            {"Joyce Akinyi",    "NID002", "Female", "28", "Mombasa",      "Finance",       "4200",  "18000"},
            {"Peter Mwangi",    "NID003", "Male",   "45", "Nairobi",      "Agriculture",   "2800",  "12000"},
            {"Grace Njeru",     "NID004", "Female", "35", "Kampala",      "Finance",       "6100",  "30000"},
            {"David Kariuki",   "NID005", "Male",   "29", "Dar es Salaam","Agriculture",   "1800",   "7000"},
            {"Amina Hassan",    "NID006", "Female", "41", "Nairobi",      "Technology",    "7200",  "40000"},
            {"James Otieno",    "NID007", "Male",   "38", "Nairobi",      "Agriculture",   "2200",   "9000"},
            {"Mary Wanjiku",    "NID008", "Female", "31", "Mombasa",      "Finance",       "3900",  "16000"},
            {"Charles Mutua",   "NID009", "Male",   "26", "Kampala",      "Technology",    "4800",  "22000"},
            {"Fatuma Ali",      "NID010", "Female", "44", "Nairobi",      "Agriculture",   "1500",   "5500"}
        };

        Borrower[] borrowers = new Borrower[borrowerData.length];
        for (int i = 0; i < borrowerData.length; i++) {
            String[] d = borrowerData[i];
            Borrower b = new Borrower();
            b.setFullName(d[0]); b.setNationalId(d[1]); b.setGender(d[2]);
            b.setAge(Integer.parseInt(d[3])); b.setLocation(d[4]);
            b.setBusinessSector(d[5]);
            b.setMonthlyIncome(Double.parseDouble(d[6]));
            b.setCollateralValue(Double.parseDouble(d[7]));
            b.setInstitutionId(instId);
            b.setCreatedAt(LocalDateTime.now().minusMonths(9 - (i % 9)));
            borrowers[i] = borrowerRepository.save(b);
        }

        // Loans
        Object[][] loanData = {
            {0, 15000.0, 14.0, 18, "Active",    -8},
            {1, 22000.0, 12.5, 24, "Active",    -6},
            {2,  8000.0, 16.0, 12, "Defaulted", -14},
            {3, 35000.0, 11.0, 36, "Active",    -4},
            {4,  5000.0, 18.0,  6, "Closed",    -16},
            {5, 50000.0, 10.5, 48, "Active",    -2},
            {6,  7500.0, 15.0, 12, "Defaulted", -12},
            {7, 12000.0, 13.5, 18, "Active",    -5},
            {8, 20000.0, 12.0, 24, "Active",    -3},
            {9,  4000.0, 17.0,  6, "Closed",    -18}
        };

        Loan[] loans = new Loan[loanData.length];
        for (int i = 0; i < loanData.length; i++) {
            Object[] d = loanData[i];
            Loan l = new Loan();
            l.setBorrowerId(borrowers[(int) d[0]].getBorrowerId());
            l.setInstitutionId(instId);
            l.setLoanAmount((Double) d[1]);
            l.setInterestRate((Double) d[2]);
            l.setTenureMonths((Integer) d[3]);
            l.setStatus((String) d[4]);
            l.setDisbursementDate(LocalDateTime.now().plusMonths((int) d[5]));
            l.setCreatedAt(LocalDateTime.now().plusMonths((int) d[5]));
            loans[i] = loanRepository.save(l);
        }

        // Repayments (2 per loan)
        for (int i = 0; i < loans.length; i++) {
            Loan l = loans[i];
            boolean isDefaulted = "Defaulted".equals(l.getStatus());

            Repayment r1 = new Repayment();
            r1.setLoanId(l.getLoanId());
            r1.setDueDate(LocalDate.now().minusMonths(2));
            r1.setPaymentDate(LocalDate.now().minusMonths(2).plusDays(isDefaulted ? 35 : 2));
            r1.setAmountDue(l.getLoanAmount() / l.getTenureMonths());
            r1.setAmountPaid(isDefaulted ? r1.getAmountDue() * 0.5 : r1.getAmountDue());
            r1.setDaysPastDue(isDefaulted ? 35 : 0);
            repaymentRepository.save(r1);

            Repayment r2 = new Repayment();
            r2.setLoanId(l.getLoanId());
            r2.setDueDate(LocalDate.now().minusMonths(1));
            r2.setPaymentDate(isDefaulted ? null : LocalDate.now().minusMonths(1).plusDays(1));
            r2.setAmountDue(l.getLoanAmount() / l.getTenureMonths());
            r2.setAmountPaid(isDefaulted ? 0.0 : r2.getAmountDue());
            r2.setDaysPastDue(isDefaulted ? 30 : 0);
            repaymentRepository.save(r2);
        }

        // Risk Scores
        for (int i = 0; i < loans.length; i++) {
            RiskScore rs = riskCalculationService.calculateRiskScore(loans[i], borrowers[(int) loanData[i][0]]);
            rs.setCreatedAt(loans[i].getCreatedAt().plusDays(1));
            riskScoreRepository.save(rs);
        }

        // Portfolio Metrics
        PortfolioMetrics pm = new PortfolioMetrics();
        pm.setInstitutionId(instId);
        pm.setPar30(3.8);
        pm.setPar90(1.5);
        pm.setSectorConcentration(0.42);
        pm.setRegionRiskIndex(0.58);
        pm.setForecastDefaultRate(0.07);
        pm.setGeneratedAt(LocalDateTime.now());
        portfolioMetricsRepository.save(pm);

        // Audit Logs
        String[][] auditData = {
            {"CREATE", "Loan",     "1"},
            {"UPDATE", "Loan",     "3"},
            {"CREATE", "Borrower", "2"},
            {"DELETE", "RiskScore","5"},
            {"CREATE", "User",     "1"}
        };
        for (String[] d : auditData) {
            AuditLog al = new AuditLog();
            al.setUserId(admin.getUserId());
            al.setAction(d[0]);
            al.setEntityType(d[1]);
            al.setEntityId(Long.parseLong(d[2]));
            al.setTimestamp(LocalDateTime.now().minusDays((int)(Math.random() * 30)));
            al.setIpAddress("127.0.0.1"); // demo/seed data placeholder
            auditLogRepository.save(al);
        }
    }
}

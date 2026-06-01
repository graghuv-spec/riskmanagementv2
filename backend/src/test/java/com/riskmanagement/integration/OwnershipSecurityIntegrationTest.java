package com.riskmanagement.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riskmanagement.model.Borrower;
import com.riskmanagement.model.Institution;
import com.riskmanagement.model.User;
import com.riskmanagement.repository.BorrowerRepository;
import com.riskmanagement.repository.InstitutionRepository;
import com.riskmanagement.repository.LoanRepository;
import com.riskmanagement.repository.UserRepository;
import com.riskmanagement.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OwnershipSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private InstitutionRepository institutionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BorrowerRepository borrowerRepository;

    @Autowired
    private LoanRepository loanRepository;

    @BeforeEach
    void cleanState() {
        loanRepository.deleteAll();
        borrowerRepository.deleteAll();
        userRepository.deleteAll();
        institutionRepository.deleteAll();
    }

    @Test
    void profileEndpointReturnsLoggedInUserBorrowerProfile() throws Exception {
        Institution institution = saveInstitution("Profile Bank", "LIC-PROFILE");
        User user = saveUser(institution.getInstitutionId(), "Alice Profile", "alice.profile@test.com");
        Borrower borrower = saveBorrower(user, institution.getInstitutionId(), "Alice Profile", "NID-PROFILE-001");

        String token = jwtService.generateToken(user);

        mockMvc.perform(get("/api/borrowers/me/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.borrowerId").value(borrower.getBorrowerId()))
                .andExpect(jsonPath("$.fullName").value("Alice Profile"))
                .andExpect(jsonPath("$.monthlyIncome").value(4200.0));
    }

    @Test
    void profileEndpointRejectsUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/borrowers/me/profile"))
                .andExpect(status().isForbidden());
    }

    @Test
    void createLoanRejectsBorrowerOwnedByDifferentUser() throws Exception {
        Institution institution = saveInstitution("Ownership Bank", "LIC-OWNER");
        User owner = saveUser(institution.getInstitutionId(), "Owner User", "owner@test.com");
        User intruder = saveUser(institution.getInstitutionId(), "Intruder User", "intruder@test.com");

        Borrower ownerBorrower = saveBorrower(owner, institution.getInstitutionId(), "Owner User", "NID-OWNER-001");
        String intruderToken = jwtService.generateToken(intruder);

        Map<String, Object> payload = new HashMap<>();
        payload.put("borrowerId", ownerBorrower.getBorrowerId());
        payload.put("institutionId", institution.getInstitutionId());
        payload.put("loanAmount", 9000.0);
        payload.put("interestRate", 11.5);
        payload.put("tenureMonths", 18);
        payload.put("status", "Active");
        payload.put("disbursementDate", LocalDateTime.now().toString());

        mockMvc.perform(post("/api/loans")
                        .header("Authorization", "Bearer " + intruderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createLoanAcceptsBorrowerOwnedByLoggedInUser() throws Exception {
        Institution institution = saveInstitution("Success Bank", "LIC-SUCCESS");
        User user = saveUser(institution.getInstitutionId(), "Success User", "success@test.com");
        Borrower borrower = saveBorrower(user, institution.getInstitutionId(), "Success User", "NID-SUCCESS-001");

        String token = jwtService.generateToken(user);

        Map<String, Object> payload = new HashMap<>();
        payload.put("borrowerId", borrower.getBorrowerId());
        payload.put("institutionId", 99999);
        payload.put("loanAmount", 13000.0);
        payload.put("interestRate", 12.0);
        payload.put("tenureMonths", 24);
        payload.put("status", "Active");
        payload.put("disbursementDate", LocalDateTime.now().toString());

        mockMvc.perform(post("/api/loans")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.borrowerId").value(borrower.getBorrowerId()))
                .andExpect(jsonPath("$.institutionId").value(institution.getInstitutionId()));
    }

    @Test
    void graphqlCreateLoanRejectsBorrowerOwnedByDifferentUser() throws Exception {
        Institution institution = saveInstitution("GraphQL Bank", "LIC-GQL");
        User owner = saveUser(institution.getInstitutionId(), "Graph Owner", "graph.owner@test.com");
        User intruder = saveUser(institution.getInstitutionId(), "Graph Intruder", "graph.intruder@test.com");

        Borrower ownerBorrower = saveBorrower(owner, institution.getInstitutionId(), "Graph Owner", "NID-GQL-OWNER");
        String intruderToken = jwtService.generateToken(intruder);

        String mutation = "mutation CreateLoan($loan: LoanInput!) { createLoan(loan: $loan) { loanId borrowerId institutionId } }";

        Map<String, Object> variables = new HashMap<>();
        Map<String, Object> loan = new HashMap<>();
        loan.put("borrowerId", ownerBorrower.getBorrowerId());
        loan.put("institutionId", institution.getInstitutionId());
        loan.put("loanAmount", 9500.0);
        loan.put("interestRate", 13.0);
        loan.put("tenureMonths", 12);
        loan.put("status", "Active");
        loan.put("disbursementDate", LocalDateTime.now().toString());
        variables.put("loan", loan);

        Map<String, Object> request = new HashMap<>();
        request.put("query", mutation);
        request.put("variables", variables);

        mockMvc.perform(post("/graphql")
                        .header("Authorization", "Bearer " + intruderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors[0].message", containsString("INTERNAL_ERROR")))
            .andExpect(jsonPath("$.errors[0].path[0]").value("createLoan"));
    }

    @Test
    void graphqlCreateLoanRejectsUnauthenticatedRequest() throws Exception {
        String mutation = "mutation CreateLoan($loan: LoanInput!) { createLoan(loan: $loan) { loanId borrowerId institutionId } }";

        Map<String, Object> variables = new HashMap<>();
        Map<String, Object> loan = new HashMap<>();
        loan.put("borrowerId", 1L);
        loan.put("institutionId", 1L);
        loan.put("loanAmount", 5000.0);
        loan.put("interestRate", 10.0);
        loan.put("tenureMonths", 12);
        loan.put("status", "Active");
        loan.put("disbursementDate", LocalDateTime.now().toString());
        variables.put("loan", loan);

        Map<String, Object> request = new HashMap<>();
        request.put("query", mutation);
        request.put("variables", variables);

        mockMvc.perform(post("/graphql")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    private Institution saveInstitution(String name, String license) {
        Institution institution = new Institution();
        institution.setName(name);
        institution.setLicenseNumber(license);
        institution.setContactEmail(name.toLowerCase().replace(" ", "") + "@test.com");
        institution.setSubscriptionPlan("Standard");
        institution.setCreatedAt(LocalDateTime.now());
        return institutionRepository.save(institution);
    }

    private User saveUser(Long institutionId, String name, String email) {
        User user = new User();
        user.setInstitutionId(institutionId);
        user.setName(name);
        user.setEmail(email);
        user.setRole("Admin");
        user.setPasswordHash("hashed-password");
        user.setMfaEnabled(false);
        user.setCreatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    private Borrower saveBorrower(User user, Long institutionId, String fullName, String nationalId) {
        Borrower borrower = new Borrower();
        borrower.setUserId(user.getUserId());
        borrower.setInstitutionId(institutionId);
        borrower.setFullName(fullName);
        borrower.setNationalId(nationalId);
        borrower.setGender("Male");
        borrower.setAge(34);
        borrower.setLocation("Nairobi");
        borrower.setBusinessSector("Finance");
        borrower.setMonthlyIncome(4200.0);
        borrower.setCollateralValue(18000.0);
        borrower.setCreatedAt(LocalDateTime.now());
        return borrowerRepository.save(borrower);
    }
}

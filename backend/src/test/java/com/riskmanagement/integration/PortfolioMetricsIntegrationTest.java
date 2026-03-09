package com.riskmanagement.integration;

import com.riskmanagement.model.Institution;
import com.riskmanagement.model.PortfolioMetrics;
import com.riskmanagement.service.InstitutionService;
import com.riskmanagement.service.PortfolioMetricsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
class PortfolioMetricsIntegrationTest {

    @Autowired
    private PortfolioMetricsService portfolioMetricsService;

    @Autowired
    private InstitutionService institutionService;

    private Institution savedInstitution() {
        Institution inst = new Institution();
        inst.setName("Metrics Bank");
        inst.setLicenseNumber("LIC-METRICS");
        inst.setContactEmail("metrics@test.com");
        inst.setSubscriptionPlan("Premium");
        inst.setCreatedAt(LocalDateTime.now());
        return institutionService.saveInstitution(inst);
    }

    private PortfolioMetrics buildMetrics(Long institutionId) {
        PortfolioMetrics m = new PortfolioMetrics();
        m.setInstitutionId(institutionId);
        m.setPar30(3.5);
        m.setPar90(1.2);
        m.setSectorConcentration(0.45);
        m.setRegionRiskIndex(0.60);
        m.setForecastDefaultRate(0.08);
        m.setGeneratedAt(LocalDateTime.now());
        return m;
    }

    @Test
    void testCreate() {
        Institution inst = savedInstitution();
        PortfolioMetrics saved = portfolioMetricsService.savePortfolioMetrics(buildMetrics(inst.getInstitutionId()));
        assertNotNull(saved.getMetricId());
        assertEquals(inst.getInstitutionId(), saved.getInstitutionId());
        assertEquals(3.5, saved.getPar30());
        assertEquals(1.2, saved.getPar90());
    }

    @Test
    void testRead() {
        Institution inst = savedInstitution();
        PortfolioMetrics saved = portfolioMetricsService.savePortfolioMetrics(buildMetrics(inst.getInstitutionId()));
        Optional<PortfolioMetrics> found = portfolioMetricsService.getPortfolioMetricsById(saved.getMetricId());
        assertTrue(found.isPresent());
        assertEquals(0.08, found.get().getForecastDefaultRate());
        assertEquals(0.45, found.get().getSectorConcentration());
    }

    @Test
    void testUpdate() {
        Institution inst = savedInstitution();
        PortfolioMetrics saved = portfolioMetricsService.savePortfolioMetrics(buildMetrics(inst.getInstitutionId()));
        saved.setPar30(5.0);
        saved.setForecastDefaultRate(0.12);
        PortfolioMetrics updated = portfolioMetricsService.savePortfolioMetrics(saved);
        assertEquals(5.0, updated.getPar30());
        assertEquals(0.12, updated.getForecastDefaultRate());
    }

    @Test
    void testDelete() {
        Institution inst = savedInstitution();
        PortfolioMetrics saved = portfolioMetricsService.savePortfolioMetrics(buildMetrics(inst.getInstitutionId()));
        Long id = saved.getMetricId();
        portfolioMetricsService.deletePortfolioMetrics(id);
        assertFalse(portfolioMetricsService.getPortfolioMetricsById(id).isPresent());
    }

    @Test
    void testGetAll() {
        Institution inst = savedInstitution();
        int before = portfolioMetricsService.getAllPortfolioMetrics().size();
        portfolioMetricsService.savePortfolioMetrics(buildMetrics(inst.getInstitutionId()));
        portfolioMetricsService.savePortfolioMetrics(buildMetrics(inst.getInstitutionId()));
        List<PortfolioMetrics> all = portfolioMetricsService.getAllPortfolioMetrics();
        assertEquals(before + 2, all.size());
    }
}

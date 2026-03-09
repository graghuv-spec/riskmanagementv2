package com.riskmanagement.integration;

import com.riskmanagement.model.AuditLog;
import com.riskmanagement.service.AuditLogService;
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
class AuditLogIntegrationTest {

    @Autowired
    private AuditLogService auditLogService;

    private AuditLog buildAuditLog(String action, String entityType) {
        AuditLog log = new AuditLog();
        log.setUserId(1L);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(100L);
        log.setTimestamp(LocalDateTime.now());
        log.setIpAddress("127.0.0.1");
        return log;
    }

    @Test
    void testCreate() {
        AuditLog saved = auditLogService.saveAuditLog(buildAuditLog("CREATE", "Loan"));
        assertNotNull(saved.getLogId());
        assertEquals("CREATE", saved.getAction());
        assertEquals("Loan", saved.getEntityType());
        assertEquals("127.0.0.1", saved.getIpAddress());
    }

    @Test
    void testRead() {
        AuditLog saved = auditLogService.saveAuditLog(buildAuditLog("READ", "Borrower"));
        Optional<AuditLog> found = auditLogService.getAuditLogById(saved.getLogId());
        assertTrue(found.isPresent());
        assertEquals("READ", found.get().getAction());
        assertEquals(100L, found.get().getEntityId());
    }

    @Test
    void testUpdate() {
        AuditLog saved = auditLogService.saveAuditLog(buildAuditLog("UPDATE", "User"));
        saved.setAction("DELETE");
        saved.setIpAddress("192.168.1.1");
        AuditLog updated = auditLogService.saveAuditLog(saved);
        assertEquals("DELETE", updated.getAction());
        assertEquals("192.168.1.1", updated.getIpAddress());
    }

    @Test
    void testDelete() {
        AuditLog saved = auditLogService.saveAuditLog(buildAuditLog("DELETE", "Institution"));
        Long id = saved.getLogId();
        auditLogService.deleteAuditLog(id);
        assertFalse(auditLogService.getAuditLogById(id).isPresent());
    }

    @Test
    void testGetAll() {
        int before = auditLogService.getAllAuditLogs().size();
        auditLogService.saveAuditLog(buildAuditLog("CREATE", "Loan"));
        auditLogService.saveAuditLog(buildAuditLog("UPDATE", "Loan"));
        auditLogService.saveAuditLog(buildAuditLog("DELETE", "Loan"));
        List<AuditLog> all = auditLogService.getAllAuditLogs();
        assertEquals(before + 3, all.size());
    }
}

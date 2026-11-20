package com.bhoomi.common;

import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void record(UUID actorId, String action, String entityType, UUID entityId,
            Map<String, Object> details) {
        auditLogRepository.save(new AuditLog(actorId, action, entityType, entityId, details));
    }
}

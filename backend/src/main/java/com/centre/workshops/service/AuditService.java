package com.centre.workshops.service;

import com.centre.workshops.domain.AuditLog;
import com.centre.workshops.repo.AuditLogRepository;
import com.centre.workshops.security.AuthUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditLogRepository repo;

    public AuditService(AuditLogRepository repo) {
        this.repo = repo;
    }

    public void record(AuthUser actor, String action, String entityType, Long entityId, String details) {
        AuditLog log = new AuditLog();
        log.setActorId(actor.id());
        log.setActorName(actor.name());
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        if (details != null && details.length() > 1000) {
            details = details.substring(0, 997) + "...";
        }
        log.setDetails(details);
        repo.save(log);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> list(String entityType, int page, int size) {
        PageRequest pr = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        if (entityType == null || entityType.isBlank()) {
            return repo.findAllByOrderByIdDesc(pr);
        }
        return repo.findByEntityTypeOrderByIdDesc(entityType.trim().toUpperCase(), pr);
    }
}

package com.centre.workshops.repo;

import com.centre.workshops.domain.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findAllByOrderByIdDesc(Pageable pageable);
    Page<AuditLog> findByEntityTypeOrderByIdDesc(String entityType, Pageable pageable);
}

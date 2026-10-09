package com.centre.workshops.web;

import com.centre.workshops.service.AuditService;
import com.centre.workshops.web.dto.Dtos.AuditResponse;
import com.centre.workshops.web.dto.Dtos.PageResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditService audit;

    public AuditController(AuditService audit) {
        this.audit = audit;
    }

    @GetMapping
    public PageResponse<AuditResponse> list(@RequestParam(required = false) String entityType,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "25") int size) {
        return PageResponse.from(audit.list(entityType, page, size), AuditResponse::from);
    }
}

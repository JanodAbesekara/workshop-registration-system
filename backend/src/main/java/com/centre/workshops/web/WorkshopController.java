package com.centre.workshops.web;

import com.centre.workshops.domain.WorkshopStatus;
import com.centre.workshops.security.AuthUser;
import com.centre.workshops.service.WorkshopService;
import com.centre.workshops.web.dto.Dtos.PageResponse;
import com.centre.workshops.web.dto.Dtos.WorkshopRequest;
import com.centre.workshops.web.dto.Dtos.WorkshopResponse;
import jakarta.validation.Valid;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workshops")
public class WorkshopController {

    private final WorkshopService workshops;

    public WorkshopController(WorkshopService workshops) {
        this.workshops = workshops;
    }

    @GetMapping
    public PageResponse<WorkshopResponse> search(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) WorkshopStatus status,
            @RequestParam(defaultValue = "false") boolean hasSeats,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "asc") String sort) {
        return PageResponse.from(
                workshops.search(from, to, status, hasSeats, q, page, size, "desc".equalsIgnoreCase(sort)),
                WorkshopResponse::from);
    }

    @GetMapping("/{id}")
    public WorkshopResponse get(@PathVariable Long id) {
        return WorkshopResponse.from(workshops.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkshopResponse create(@Valid @RequestBody WorkshopRequest req, @AuthenticationPrincipal AuthUser actor) {
        return WorkshopResponse.from(workshops.create(req, actor));
    }

    @PutMapping("/{id}")
    public WorkshopResponse update(@PathVariable Long id, @Valid @RequestBody WorkshopRequest req,
                                   @AuthenticationPrincipal AuthUser actor) {
        return WorkshopResponse.from(workshops.update(id, req, actor));
    }
}

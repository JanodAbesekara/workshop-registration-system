package com.centre.workshops.service;

import com.centre.workshops.domain.Workshop;
import com.centre.workshops.domain.WorkshopStatus;
import com.centre.workshops.exception.ApiException;
import com.centre.workshops.repo.WorkshopRepository;
import com.centre.workshops.security.AuthUser;
import com.centre.workshops.web.dto.Dtos.WorkshopRequest;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkshopService {

    private final WorkshopRepository workshops;
    private final AuditService audit;

    public WorkshopService(WorkshopRepository workshops, AuditService audit) {
        this.workshops = workshops;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public Page<Workshop> search(Instant from, Instant to, WorkshopStatus status, boolean onlyWithSeats,
                                 String q, int page, int size, boolean newestFirst) {
        Specification<Workshop> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (from != null) {
                p.add(cb.greaterThanOrEqualTo(root.<Instant>get("startsAt"), from));
            }
            if (to != null) {
                p.add(cb.lessThanOrEqualTo(root.<Instant>get("startsAt"), to));
            }
            if (status != null) {
                p.add(cb.equal(root.get("status"), status));
            }
            if (onlyWithSeats) {
                p.add(cb.lessThan(root.<Integer>get("activeCount"), root.<Integer>get("capacity")));
            }
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
                p.add(cb.or(
                        cb.like(cb.lower(root.<String>get("title")), like),
                        cb.like(cb.lower(root.<String>get("code")), like),
                        cb.like(cb.lower(root.<String>get("instructor")), like)));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
        Sort sort = newestFirst
                ? Sort.by(Sort.Order.desc("startsAt"), Sort.Order.desc("id"))
                : Sort.by(Sort.Order.asc("startsAt"), Sort.Order.asc("id"));
        PageRequest pr = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), sort);
        return workshops.findAll(spec, pr);
    }

    @Transactional(readOnly = true)
    public Workshop get(Long id) {
        return workshops.findById(id).orElseThrow(() -> ApiException.notFound("Workshop not found."));
    }

    @Transactional
    public Workshop create(WorkshopRequest r, AuthUser actor) {
        validateTimes(r);
        String code = normaliseCode(r.code());
        if (workshops.existsByCodeIgnoreCase(code)) {
            throw ApiException.conflict("A workshop with code " + code + " already exists.");
        }
        Workshop w = new Workshop();
        w.setCode(code);
        apply(w, r);
        w.setStatus(r.status() != null ? r.status() : WorkshopStatus.SCHEDULED);
        w.setActiveCount(0);
        w = workshops.saveAndFlush(w);
        audit.record(actor, "WORKSHOP_CREATED", "WORKSHOP", w.getId(),
                code + " \"" + w.getTitle() + "\", capacity " + w.getCapacity());
        return w;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Workshop update(Long id, WorkshopRequest r, AuthUser actor) {
        validateTimes(r);
        Workshop w = workshops.findByIdForUpdate(id).orElseThrow(() -> ApiException.notFound("Workshop not found."));
        String code = normaliseCode(r.code());
        if (!w.getCode().equalsIgnoreCase(code) && workshops.existsByCodeIgnoreCase(code)) {
            throw ApiException.conflict("A workshop with code " + code + " already exists.");
        }
        if (r.capacity() < w.getActiveCount()) {
            throw ApiException.conflict("Capacity cannot be lower than the " + w.getActiveCount()
                    + " active registrations. Cancel some registrations first.");
        }
        List<String> changes = new ArrayList<>();
        diff(changes, "code", w.getCode(), code);
        diff(changes, "title", w.getTitle(), r.title().trim());
        diff(changes, "instructor", w.getInstructor(), r.instructor().trim());
        diff(changes, "location", w.getLocation(), blankToNull(r.location()));
        diff(changes, "description", w.getDescription(), blankToNull(r.description()));
        diff(changes, "startsAt", w.getStartsAt(), r.startsAt());
        diff(changes, "endsAt", w.getEndsAt(), r.endsAt());
        diff(changes, "capacity", w.getCapacity(), r.capacity());
        WorkshopStatus newStatus = r.status() != null ? r.status() : w.getStatus();
        diff(changes, "status", w.getStatus(), newStatus);

        w.setCode(code);
        apply(w, r);
        w.setStatus(newStatus);
        if (!changes.isEmpty()) {
            audit.record(actor, "WORKSHOP_UPDATED", "WORKSHOP", w.getId(), w.getCode() + " | " + String.join("; ", changes));
        }
        return w;
    }

    private void apply(Workshop w, WorkshopRequest r) {
        w.setTitle(r.title().trim());
        w.setInstructor(r.instructor().trim());
        w.setLocation(blankToNull(r.location()));
        w.setDescription(blankToNull(r.description()));
        w.setStartsAt(r.startsAt());
        w.setEndsAt(r.endsAt());
        w.setCapacity(r.capacity());
    }

    private void validateTimes(WorkshopRequest r) {
        if (!r.endsAt().isAfter(r.startsAt())) {
            throw ApiException.badRequest("End time must be after the start time.");
        }
    }

    private String normaliseCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private void diff(List<String> out, String field, Object before, Object after) {
        if (!Objects.equals(before, after)) {
            out.add(field + ": " + before + " -> " + after);
        }
    }
}

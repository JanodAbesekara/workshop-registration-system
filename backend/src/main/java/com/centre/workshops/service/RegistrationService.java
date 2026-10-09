package com.centre.workshops.service;

import com.centre.workshops.domain.Registration;
import com.centre.workshops.domain.RegistrationStatus;
import com.centre.workshops.domain.Workshop;
import com.centre.workshops.domain.WorkshopStatus;
import com.centre.workshops.exception.ApiException;
import com.centre.workshops.repo.RegistrationRepository;
import com.centre.workshops.repo.WorkshopRepository;
import com.centre.workshops.security.AuthUser;
import com.centre.workshops.web.dto.Dtos.CancelRequest;
import com.centre.workshops.web.dto.Dtos.RegisterRequest;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {

    private final WorkshopRepository workshops;
    private final RegistrationRepository registrations;

    public RegistrationService(WorkshopRepository workshops, RegistrationRepository registrations) {
        this.workshops = workshops;
        this.registrations = registrations;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Registration register(Long workshopId, RegisterRequest req, AuthUser actor) {
        Workshop w = workshops.findByIdForUpdate(workshopId)
                .orElseThrow(() -> ApiException.notFound("Workshop not found."));

        if (w.getStatus() != WorkshopStatus.SCHEDULED) {
            throw ApiException.conflict("Registration is closed: this workshop is " + w.getStatus().name().toLowerCase(Locale.ROOT) + ".");
        }
        if (!Instant.now().isBefore(w.getEndsAt())) {
            throw ApiException.conflict("Registration is closed: this workshop has already ended.");
        }
        String email = req.attendeeEmail().trim().toLowerCase(Locale.ROOT);
        if (registrations.existsByWorkshopIdAndAttendeeEmailIgnoreCaseAndStatus(workshopId, email, RegistrationStatus.ACTIVE)) {
            throw ApiException.conflict(email + " is already registered for this workshop.");
        }
        if (registrations.existsByWorkshopIdAndAttendeeEmailIgnoreCaseAndStatus(workshopId, email, RegistrationStatus.WAITLISTED)) {
            throw ApiException.conflict(email + " is already on the waitlist for this workshop.");
        }

        boolean isFull = w.getActiveCount() >= w.getCapacity();
        if (isFull && !Boolean.TRUE.equals(req.waitlist())) {
            throw ApiException.conflict("This workshop is full (" + w.getCapacity() + " of " + w.getCapacity() + " seats taken).");
        }

        Registration r = new Registration();
        r.setWorkshop(w);
        r.setAttendeeName(req.attendeeName().trim());
        r.setAttendeeEmail(email);
        r.setStatus(isFull ? RegistrationStatus.WAITLISTED : RegistrationStatus.ACTIVE);
        r.setRegisteredById(actor.id());
        r.setRegisteredByName(actor.name());
        r = registrations.save(r);

        if (!isFull) {
            w.setActiveCount(w.getActiveCount() + 1);
        }
        return r;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Registration cancel(Long workshopId, Long registrationId, CancelRequest req, AuthUser actor) {
        Workshop w = workshops.findByIdForUpdate(workshopId)
                .orElseThrow(() -> ApiException.notFound("Workshop not found."));
        Registration r = registrations.findByIdAndWorkshopId(registrationId, workshopId)
                .orElseThrow(() -> ApiException.notFound("Registration not found."));
        if (r.getStatus() == RegistrationStatus.CANCELLED) {
            throw ApiException.conflict("This registration is already cancelled.");
        }
        boolean wasActive = r.getStatus() == RegistrationStatus.ACTIVE;
        r.setStatus(RegistrationStatus.CANCELLED);
        r.setCancelledById(actor.id());
        r.setCancelledByName(actor.name());
        r.setCancelledAt(Instant.now());
        String reason = req == null || req.reason() == null ? null : req.reason().trim();
        r.setCancelReason(reason == null || reason.isEmpty() ? null : reason);
        r = registrations.save(r);

        if (wasActive) {
            var nextInLine = registrations.findFirstByWorkshopIdAndStatusOrderByRegisteredAtAscIdAsc(workshopId, RegistrationStatus.WAITLISTED);
            if (nextInLine.isPresent()) {
                Registration promoted = nextInLine.get();
                promoted.setStatus(RegistrationStatus.ACTIVE);
                registrations.save(promoted);
            } else {
                w.setActiveCount(Math.max(0, w.getActiveCount() - 1));
            }
        }
        return r;
    }

    @Transactional(readOnly = true)
    public List<Registration> history(Long workshopId, RegistrationStatus status) {
        if (!workshops.existsById(workshopId)) {
            throw ApiException.notFound("Workshop not found.");
        }
        return status == null
                ? registrations.findByWorkshopIdOrderByRegisteredAtDescIdDesc(workshopId)
                : registrations.findByWorkshopIdAndStatusOrderByRegisteredAtDescIdDesc(workshopId, status);
    }
}

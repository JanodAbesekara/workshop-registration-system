package com.centre.workshops.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "registrations",
        indexes = {
                @Index(name = "idx_reg_workshop_status", columnList = "workshop_id,status"),
                @Index(name = "idx_reg_workshop_email", columnList = "workshop_id,attendee_email")
        })
public class Registration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workshop_id", nullable = false)
    private Workshop workshop;

    @Column(name = "attendee_name", nullable = false, length = 120)
    private String attendeeName;

    @Column(name = "attendee_email", nullable = false, length = 190)
    private String attendeeEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(20)")
    private RegistrationStatus status = RegistrationStatus.ACTIVE;

    @Column(name = "registered_by_id", nullable = false)
    private Long registeredById;

    @Column(name = "registered_by_name", nullable = false, length = 100)
    private String registeredByName;

    @Column(name = "registered_at", nullable = false)
    private Instant registeredAt = Instant.now();

    @Column(name = "cancelled_by_id")
    private Long cancelledById;

    @Column(name = "cancelled_by_name", length = 100)
    private String cancelledByName;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancel_reason", length = 300)
    private String cancelReason;

    public Long getId() { return id; }
    public Workshop getWorkshop() { return workshop; }
    public void setWorkshop(Workshop workshop) { this.workshop = workshop; }
    public String getAttendeeName() { return attendeeName; }
    public void setAttendeeName(String attendeeName) { this.attendeeName = attendeeName; }
    public String getAttendeeEmail() { return attendeeEmail; }
    public void setAttendeeEmail(String attendeeEmail) { this.attendeeEmail = attendeeEmail; }
    public RegistrationStatus getStatus() { return status; }
    public void setStatus(RegistrationStatus status) { this.status = status; }
    public Long getRegisteredById() { return registeredById; }
    public void setRegisteredById(Long registeredById) { this.registeredById = registeredById; }
    public String getRegisteredByName() { return registeredByName; }
    public void setRegisteredByName(String registeredByName) { this.registeredByName = registeredByName; }
    public Instant getRegisteredAt() { return registeredAt; }
    public Long getCancelledById() { return cancelledById; }
    public void setCancelledById(Long cancelledById) { this.cancelledById = cancelledById; }
    public String getCancelledByName() { return cancelledByName; }
    public void setCancelledByName(String cancelledByName) { this.cancelledByName = cancelledByName; }
    public Instant getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(Instant cancelledAt) { this.cancelledAt = cancelledAt; }
    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }
}

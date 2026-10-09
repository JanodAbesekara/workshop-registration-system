package com.centre.workshops.web.dto;

import com.centre.workshops.domain.AuditLog;
import com.centre.workshops.domain.Registration;
import com.centre.workshops.domain.RegistrationStatus;
import com.centre.workshops.domain.Role;
import com.centre.workshops.domain.User;
import com.centre.workshops.domain.Workshop;
import com.centre.workshops.domain.WorkshopStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.data.domain.Page;

public final class Dtos {

    private Dtos() {}

    // common 
    public record ApiError(Instant timestamp, int status, String error, String message, Map<String, String> fieldErrors) {}

    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
        public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper) {
            return new PageResponse<>(
                    page.getContent().stream().map(mapper).toList(),
                    page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
        }
    }

    //  auth / users 
    public record LoginRequest(
            @NotBlank(message = "Email is required") @Email(message = "Enter a valid email") String email,
            @NotBlank(message = "Password is required") String password) {}

    public record LoginResponse(String token, Instant expiresAt, UserResponse user) {}

    public record UserResponse(Long id, String name, String email, Role role, boolean active, Instant createdAt) {
        public static UserResponse from(User u) {
            return new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getRole(), u.isActive(), u.getCreatedAt());
        }
    }

    public record CreateUserRequest(
            @NotBlank(message = "Name is required") @Size(max = 100) String name,
            @NotBlank(message = "Email is required") @Email(message = "Enter a valid email") @Size(max = 190) String email,
            @NotBlank(message = "Password is required")
            @Size(min = 8, max = 72, message = "Password must be 8 to 72 characters") String password,
            @NotNull(message = "Role is required") Role role) {}

    public record UpdateUserRequest(
            @NotBlank(message = "Name is required") @Size(max = 100) String name,
            @NotNull(message = "Role is required") Role role,
            @NotNull(message = "Active flag is required") Boolean active) {}

    public record ResetPasswordRequest(
            @NotBlank(message = "Password is required")
            @Size(min = 8, max = 72, message = "Password must be 8 to 72 characters") String password) {}

    //  workshops 
    public record WorkshopRequest(
            @NotBlank(message = "Code is required")
            @Pattern(regexp = "^[A-Za-z0-9-]{3,30}$", message = "Use 3-30 letters, digits or dashes") String code,
            @NotBlank(message = "Title is required") @Size(max = 150) String title,
            @NotBlank(message = "Instructor is required") @Size(max = 120) String instructor,
            @Size(max = 120) String location,
            @Size(max = 1000) String description,
            @NotNull(message = "Start time is required") Instant startsAt,
            @NotNull(message = "End time is required") Instant endsAt,
            @NotNull(message = "Capacity is required")
            @Min(value = 1, message = "Capacity must be at least 1")
            @Max(value = 1000, message = "Capacity must be at most 1000") Integer capacity,
            WorkshopStatus status) {}

    public record WorkshopResponse(
            Long id, String code, String title, String instructor, String location, String description,
            Instant startsAt, Instant endsAt, int capacity, int activeCount, int seatsAvailable,
            WorkshopStatus status, Instant createdAt, Instant updatedAt) {
        public static WorkshopResponse from(Workshop w) {
            return new WorkshopResponse(w.getId(), w.getCode(), w.getTitle(), w.getInstructor(), w.getLocation(),
                    w.getDescription(), w.getStartsAt(), w.getEndsAt(), w.getCapacity(), w.getActiveCount(),
                    Math.max(0, w.getCapacity() - w.getActiveCount()), w.getStatus(), w.getCreatedAt(), w.getUpdatedAt());
        }
    }

    // registrations
    public record RegisterRequest(
            @NotBlank(message = "Attendee name is required") @Size(max = 120) String attendeeName,
            @NotBlank(message = "Attendee email is required") @Email(message = "Enter a valid email")
            @Size(max = 190) String attendeeEmail,
            Boolean waitlist) {
        public RegisterRequest(String attendeeName, String attendeeEmail) {
            this(attendeeName, attendeeEmail, false);
        }
    }

    public record CancelRequest(@Size(max = 300, message = "Reason is too long") String reason) {}

    public record RegistrationResponse(
            Long id, String attendeeName, String attendeeEmail, RegistrationStatus status,
            Long registeredById, String registeredByName, Instant registeredAt,
            Long cancelledById, String cancelledByName, Instant cancelledAt, String cancelReason) {
        public static RegistrationResponse from(Registration r) {
            return new RegistrationResponse(r.getId(), r.getAttendeeName(), r.getAttendeeEmail(), r.getStatus(),
                    r.getRegisteredById(), r.getRegisteredByName(), r.getRegisteredAt(),
                    r.getCancelledById(), r.getCancelledByName(), r.getCancelledAt(), r.getCancelReason());
        }
    }

    // audit 
    public record AuditResponse(Long id, Long actorId, String actorName, String action, String entityType,
                                Long entityId, String details, Instant createdAt) {
        public static AuditResponse from(AuditLog a) {
            return new AuditResponse(a.getId(), a.getActorId(), a.getActorName(), a.getAction(), a.getEntityType(),
                    a.getEntityId(), a.getDetails(), a.getCreatedAt());
        }
    }
}

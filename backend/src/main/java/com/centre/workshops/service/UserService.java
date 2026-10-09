package com.centre.workshops.service;

import com.centre.workshops.domain.User;
import com.centre.workshops.exception.ApiException;
import com.centre.workshops.repo.UserRepository;
import com.centre.workshops.security.AuthUser;
import com.centre.workshops.web.dto.Dtos.CreateUserRequest;
import com.centre.workshops.web.dto.Dtos.ResetPasswordRequest;
import com.centre.workshops.web.dto.Dtos.UpdateUserRequest;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AuditService audit;

    public UserService(UserRepository users, PasswordEncoder encoder, AuditService audit) {
        this.users = users;
        this.encoder = encoder;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<User> list() {
        return users.findAll(Sort.by("name").ascending().and(Sort.by("id")));
    }

    @Transactional
    public User create(CreateUserRequest req, AuthUser actor) {
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("An account with this email already exists.");
        }
        User u = new User();
        u.setName(req.name().trim());
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(req.password()));
        u.setRole(req.role());
        u.setActive(true);
        u = users.save(u);
        audit.record(actor, "USER_CREATED", "USER", u.getId(),
                "Created " + email + " with role " + u.getRole());
        return u;
    }

    @Transactional
    public User update(Long id, UpdateUserRequest req, AuthUser actor) {
        User u = users.findById(id).orElseThrow(() -> ApiException.notFound("User not found."));
        boolean self = u.getId().equals(actor.id());
        if (self && (req.role() != u.getRole() || !req.active())) {
            throw ApiException.conflict("You cannot change your own role or deactivate your own account.");
        }
        List<String> changes = new ArrayList<>();
        if (!u.getName().equals(req.name().trim())) {
            changes.add("name: " + u.getName() + " -> " + req.name().trim());
            u.setName(req.name().trim());
        }
        if (u.getRole() != req.role()) {
            changes.add("role: " + u.getRole() + " -> " + req.role());
            u.setRole(req.role());
        }
        if (u.isActive() != req.active()) {
            changes.add("active: " + u.isActive() + " -> " + req.active());
            u.setActive(req.active());
        }
        if (!changes.isEmpty()) {
            audit.record(actor, "USER_UPDATED", "USER", u.getId(), u.getEmail() + " | " + String.join("; ", changes));
        }
        return u;
    }

    @Transactional
    public void resetPassword(Long id, ResetPasswordRequest req, AuthUser actor) {
        User u = users.findById(id).orElseThrow(() -> ApiException.notFound("User not found."));
        u.setPasswordHash(encoder.encode(req.password()));
        audit.record(actor, "USER_PASSWORD_RESET", "USER", u.getId(), "Password reset for " + u.getEmail());
    }
}

package com.centre.workshops.web;

import com.centre.workshops.domain.RegistrationStatus;
import com.centre.workshops.security.AuthUser;
import com.centre.workshops.service.RegistrationService;
import com.centre.workshops.web.dto.Dtos.CancelRequest;
import com.centre.workshops.web.dto.Dtos.RegisterRequest;
import com.centre.workshops.web.dto.Dtos.RegistrationResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workshops/{workshopId}/registrations")
public class RegistrationController {

    private final RegistrationService registrations;

    public RegistrationController(RegistrationService registrations) {
        this.registrations = registrations;
    }

    @GetMapping
    public List<RegistrationResponse> history(@PathVariable Long workshopId,
                                              @RequestParam(required = false) RegistrationStatus status) {
        return registrations.history(workshopId, status).stream().map(RegistrationResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RegistrationResponse register(@PathVariable Long workshopId, @Valid @RequestBody RegisterRequest req,
                                         @AuthenticationPrincipal AuthUser actor) {
        return RegistrationResponse.from(registrations.register(workshopId, req, actor));
    }

    @PostMapping("/{registrationId}/cancel")
    public RegistrationResponse cancel(@PathVariable Long workshopId, @PathVariable Long registrationId,
                                       @Valid @RequestBody(required = false) CancelRequest req,
                                       @AuthenticationPrincipal AuthUser actor) {
        return RegistrationResponse.from(registrations.cancel(workshopId, registrationId, req, actor));
    }
}

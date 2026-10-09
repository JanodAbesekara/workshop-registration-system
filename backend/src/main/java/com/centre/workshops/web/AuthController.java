package com.centre.workshops.web;

import com.centre.workshops.security.AuthUser;
import com.centre.workshops.service.AuthService;
import com.centre.workshops.web.dto.Dtos.LoginRequest;
import com.centre.workshops.web.dto.Dtos.LoginResponse;
import com.centre.workshops.web.dto.Dtos.UserResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        return auth.login(req);
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AuthUser actor) {
        return auth.me(actor.id());
    }
}

package com.centre.workshops.service;

import com.centre.workshops.domain.User;
import com.centre.workshops.exception.ApiException;
import com.centre.workshops.repo.UserRepository;
import com.centre.workshops.security.TokenService;
import com.centre.workshops.security.TokenService.IssuedToken;
import com.centre.workshops.web.dto.Dtos.LoginRequest;
import com.centre.workshops.web.dto.Dtos.LoginResponse;
import com.centre.workshops.web.dto.Dtos.UserResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final String BAD_LOGIN = "Invalid email or password.";

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final TokenService tokens;
    private final LoginAttemptService attempts;
    private final String dummyHash;

    public AuthService(UserRepository users, PasswordEncoder encoder, TokenService tokens, LoginAttemptService attempts) {
        this.users = users;
        this.encoder = encoder;
        this.tokens = tokens;
        this.attempts = attempts;
        this.dummyHash = encoder.encode("not-a-real-password");
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest req) {
        String key = req.email().trim().toLowerCase();
        attempts.checkAllowed(key);

        User user = users.findByEmailIgnoreCase(key).orElse(null);
        boolean ok = encoder.matches(req.password(), user != null ? user.getPasswordHash() : dummyHash);
        if (user == null || !ok || !user.isActive()) {
            attempts.recordFailure(key);
            throw ApiException.unauthorized(BAD_LOGIN);
        }
        attempts.recordSuccess(key);
        IssuedToken token = tokens.issue(user);
        return new LoginResponse(token.value(), token.expiresAt(), UserResponse.from(user));
    }

    @Transactional(readOnly = true)
    public UserResponse me(Long id) {
        return UserResponse.from(users.findById(id).orElseThrow(() -> ApiException.unauthorized("Account not found.")));
    }
}

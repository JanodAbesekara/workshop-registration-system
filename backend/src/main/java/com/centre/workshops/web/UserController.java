package com.centre.workshops.web;

import com.centre.workshops.security.AuthUser;
import com.centre.workshops.service.UserService;
import com.centre.workshops.web.dto.Dtos.CreateUserRequest;
import com.centre.workshops.web.dto.Dtos.ResetPasswordRequest;
import com.centre.workshops.web.dto.Dtos.UpdateUserRequest;
import com.centre.workshops.web.dto.Dtos.UserResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService users;

    public UserController(UserService users) {
        this.users = users;
    }

    @GetMapping
    public List<UserResponse> list() {
        return users.list().stream().map(UserResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody CreateUserRequest req, @AuthenticationPrincipal AuthUser actor) {
        return UserResponse.from(users.create(req, actor));
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest req,
                               @AuthenticationPrincipal AuthUser actor) {
        return UserResponse.from(users.update(id, req, actor));
    }

    @PostMapping("/{id}/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordRequest req,
                              @AuthenticationPrincipal AuthUser actor) {
        users.resetPassword(id, req, actor);
    }
}

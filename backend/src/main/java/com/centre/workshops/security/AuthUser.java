package com.centre.workshops.security;

import com.centre.workshops.domain.Role;

public record AuthUser(Long id, String email, String name, Role role) {}

package com.centre.workshops.security;

import com.centre.workshops.domain.User;
import com.centre.workshops.repo.UserRepository;
import java.util.List;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class JwtUserConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserRepository users;

    public JwtUserConverter(UserRepository users) {
        this.users = users;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Long id;
        try {
            id = Long.valueOf(jwt.getSubject());
        } catch (NumberFormatException ex) {
            throw new BadCredentialsException("Invalid token");
        }
        User user = users.findById(id).orElseThrow(() -> new BadCredentialsException("Account no longer exists"));
        if (!user.isActive()) {
            throw new BadCredentialsException("Account is disabled");
        }
        AuthUser principal = new AuthUser(user.getId(), user.getEmail(), user.getName(), user.getRole());
        return new UsernamePasswordAuthenticationToken(
                principal, jwt.getTokenValue(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
    }
}

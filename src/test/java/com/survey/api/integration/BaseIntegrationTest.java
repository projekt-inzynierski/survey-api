package com.survey.api.integration;

import com.survey.api.security.TokenProvider;
import com.survey.domain.models.IdentityUser;
import com.survey.domain.repository.IdentityUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

public abstract class BaseIntegrationTest {
    @Autowired
    private TokenProvider tokenProvider;
    @Autowired
    private UserDetailsService userDetailsService;
    @Autowired
    private IdentityUserRepository identityUserRepository;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private PasswordEncoder passwordEncoder;

    protected String saddUserWithRoleAndGetToken(String role){
        String randomUsername = UUID.randomUUID().toString();
        String password = "Password";
        String hash = passwordEncoder.encode(password);
        IdentityUser user = new IdentityUser(null, randomUsername, hash, role);
        identityUserRepository.save(user);
        Authentication authentication = authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(user.getUsername(),
                        password));
        String jwtToken = tokenProvider.generateToken(authentication);
        return jwtToken;
    }
}

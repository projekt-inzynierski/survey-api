package com.survey.application.services;

import com.survey.domain.models.IdentityUser;
import com.survey.domain.repository.IdentityUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private IdentityUserRepository identityUserRepository;

    @Autowired
    public UserDetailsServiceImpl(IdentityUserRepository identityUserRepository) {
        this.identityUserRepository = identityUserRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
            IdentityUser databaseUser = identityUserRepository
                    .findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("Username not found"));
            return new User(databaseUser.getUsername(), databaseUser.getPasswordHash(), mapRolesToAuthorities(databaseUser));
    }

    private List<GrantedAuthority> mapRolesToAuthorities(IdentityUser user){
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole()));
        return authorities;
    }

}

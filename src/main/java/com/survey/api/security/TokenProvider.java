package com.survey.api.security;

import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import java.util.Date;

import javax.swing.text.TableView;

@Component
public class TokenProvider {

    private final SecuritySettings securitySettings;

    @Autowired
    public TokenProvider(SecuritySettings securitySettings) {
        this.securitySettings = securitySettings;
    }

    public String generateToken(Authentication authentication){
        String username = authentication.getName();
        Date curretDate = new Date();
        int expiration = securitySettings.getExpiration();
        Date expireDate = new Date(curretDate.getTime() + expiration);

        String token = Jwts.builder()
                .setSubject(username)
                .setIssuedAt(curretDate)
                .setExpiration(expireDate)
                .signWith(SignatureAlgorithm.HS512, securitySettings.getKey())
                .compact();


        return token;
    }

    public String getUsernameFromJwt(String token){
        Claims claims = Jwts.parser()
                .setSigningKey(securitySettings.getKey())
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }

    public Jwt<Header, Claims> getJwt(String token){
        return Jwts.parser()
                .setSigningKey(securitySettings.getKey())
                .parseClaimsJwt(token);
    }

    public boolean validateToken(String token){
        try{
            Jwts.parser()
                    .setSigningKey(securitySettings.getKey())
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e){
            throw new AuthenticationCredentialsNotFoundException("Jwt was expired or incorrect");
        }
    }
}

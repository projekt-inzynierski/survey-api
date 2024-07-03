package com.survey.api.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.survey.application.dtos.LoginDto;
import com.survey.domain.models.IdentityUser;
import com.survey.domain.repository.IdentityUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.UUID;

@ExtendWith(IntegrationTestDatabaseInitializer.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
public class AuthenticationControllerTest{
    private final WebTestClient webTestClient;
    private final ObjectMapper objectMapper;
    private final IdentityUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AuthenticationControllerTest(WebTestClient webTestClient, ObjectMapper objectMapper,
                                        IdentityUserRepository identityUserRepository,
                                        PasswordEncoder passwordEncoder){

        this.webTestClient = webTestClient;
        this.objectMapper = objectMapper;
        this.userRepository = identityUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Test
    public void testLoginForNonExistingUser() throws JsonProcessingException {
        LoginDto dto = LoginDto
                .builder()
                .withUsername("this user does not exist")
                .withPassword("test password")
                .build();

        String json = objectMapper.writeValueAsString(dto);

        webTestClient.post().uri("/api/authentication/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(json)
                .exchange()
                .expectStatus()
                .isUnauthorized();
    }

    @Test
    public void testLoginForExistingUserButWrongPassword() throws JsonProcessingException{
        String randomUsername = UUID.randomUUID().toString();
        IdentityUser user = new IdentityUser(null, randomUsername, "some password hash", "ADMIN");
        userRepository.save(user);

        LoginDto dto = LoginDto
                .builder()
                .withUsername(randomUsername)
                .withPassword("wrong password")
                .build();

        String json = objectMapper.writeValueAsString(dto);

        webTestClient.post().uri("/api/authentication/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(json)
                .exchange()
                .expectStatus()
                .isUnauthorized();
    }

    @Test
    public void testLoginWIthCorrectCredentials() throws JsonProcessingException{
        String randomUsername = UUID.randomUUID().toString();
        String passwordHash = passwordEncoder.encode("password");
        IdentityUser user = new IdentityUser(null, randomUsername, passwordHash, "ADMIN");
        userRepository.save(user);

        LoginDto dto = LoginDto
                .builder()
                .withUsername(randomUsername)
                .withPassword("password")
                .build();

        String json = objectMapper.writeValueAsString(dto);

        webTestClient.post().uri("/api/authentication/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(json)
                .exchange()
                .expectStatus()
                .isOk();
    }

    @Test
    public void testLoginForEmptyUsernameAndPassword() throws JsonProcessingException{
        LoginDto dto = LoginDto
                .builder()
                .build();

        String json = objectMapper.writeValueAsString(dto);

        webTestClient.post().uri("/api/authentication/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(json)
                .exchange()
                .expectStatus()
                .isBadRequest();
    }
}

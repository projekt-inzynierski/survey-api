package com.survey.api.integration;

import com.survey.api.security.TokenProvider;
import com.survey.application.dtos.LastSensorEntryDateDto;
import com.survey.application.dtos.ResponseSensorDataDto;
import com.survey.application.dtos.SensorDataDto;
import com.survey.domain.models.IdentityUser;
import com.survey.domain.repository.IdentityUserRepository;
import com.survey.domain.repository.SensorDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(IntegrationTestDatabaseInitializer.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "ADMIN_USER_PASSWORD=testAdminPassword")
@AutoConfigureWebTestClient
public class SensorDataControllerIntegrationTest {
    private static final BigDecimal VALID_TEMPERATURE = new BigDecimal("21.5");
    private static final BigDecimal VALID_HUMIDITY = new BigDecimal("60.4");
    private final WebTestClient webTestClient;
    private final IdentityUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final SensorDataRepository sensorDataRepository;
    private final AuthenticationManager authenticationManager;

    @Autowired
    public SensorDataControllerIntegrationTest(WebTestClient webTestClient, IdentityUserRepository userRepository, PasswordEncoder passwordEncoder, TokenProvider tokenProvider, SensorDataRepository sensorDataRepository, AuthenticationManager authenticationManager) {
        this.webTestClient = webTestClient;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.sensorDataRepository = sensorDataRepository;
        this.authenticationManager = authenticationManager;
    }

    @BeforeEach
    void setUp(){
        sensorDataRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void saveSensorData_ValidData_ShouldReturnCreatedStatus(){
        IdentityUser user = createUserWithRole("Respondent");
        String token = authenticateAndGenerateToken(user);

        SensorDataDto entryDto = new SensorDataDto();
        entryDto.setDateTime(OffsetDateTime.now(UTC));
        entryDto.setTemperature(VALID_TEMPERATURE);
        entryDto.setHumidity(VALID_HUMIDITY);

        var response = webTestClient.post()
                .uri("/api/sensordata")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(List.of(entryDto))
                .exchange()
                .expectStatus().isCreated()
                .expectBodyList(ResponseSensorDataDto.class)
                .returnResult().getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response).hasSize(1);
        assertThat(response.get(0).getTemperature().compareTo(entryDto.getTemperature())).isEqualTo(0);
        assertThat(response.get(0).getHumidity().compareTo(entryDto.getHumidity())).isEqualTo(0);
        assertThat(response.get(0).getRespondentId()).isEqualTo(user.getId());

    }

    @Test
    void saveTemperatureData_InvalidInputMissingTemperatureField_ShouldReturnBadRequest(){
        IdentityUser user = createUserWithRole("Respondent");
        String token = authenticateAndGenerateToken(user);

        SensorDataDto entryDto = new SensorDataDto();
        entryDto.setDateTime(OffsetDateTime.now(UTC));
        entryDto.setHumidity(VALID_HUMIDITY);

        webTestClient.post()
                .uri("/api/sensordata")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(List.of(entryDto))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void saveSensorData_InvalidInputMissingDateTimeField_ShouldReturnBadRequest() {
        IdentityUser user = createUserWithRole("Respondent");
        String token = authenticateAndGenerateToken(user);

        SensorDataDto entryDto = new SensorDataDto();
        entryDto.setTemperature(VALID_TEMPERATURE);
        entryDto.setHumidity(VALID_HUMIDITY);

        webTestClient.post()
                .uri("/api/sensordata")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(List.of(entryDto))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void saveTemperatureData_InvalidTemperatureRange_ShouldReturnBadRequest() {
        IdentityUser user = createUserWithRole("Respondent");
        String token = authenticateAndGenerateToken(user);

        SensorDataDto entryDto = new SensorDataDto();
        entryDto.setDateTime(OffsetDateTime.now(UTC));
        entryDto.setTemperature(BigDecimal.valueOf(100.0));
        entryDto.setHumidity(VALID_HUMIDITY);

        webTestClient.post()
                .uri("/api/sensordata")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(List.of(entryDto))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void getSensorData_InvalidRange_ShouldReturnBadRequest() {
        IdentityUser user = createUserWithRole("Respondent");
        String token = authenticateAndGenerateToken(user);

        OffsetDateTime from = OffsetDateTime.now(UTC).plusDays(1);
        OffsetDateTime to = OffsetDateTime.now(UTC).minusDays(1);

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/sensordata")
                        .queryParam("from", from.toString())
                        .queryParam("to", to.toString())
                        .build())
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void saveSensorData_DuplicateEntry_ShouldReturnConflict(){
        IdentityUser user = createUserWithRole("Respondent");
        String token = authenticateAndGenerateToken(user);

        SensorDataDto entryDto = new SensorDataDto();
        entryDto.setDateTime(OffsetDateTime.now(UTC));
        entryDto.setTemperature(VALID_TEMPERATURE);
        entryDto.setHumidity(VALID_HUMIDITY);

        saveSensorData(user, entryDto);

        webTestClient.post()
                .uri("/api/sensordata")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(List.of(entryDto))
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void getSensorData_ValidRange_ShouldReturnOkStatus(){
        IdentityUser user = createUserWithRole("Respondent");
        String token = authenticateAndGenerateToken(user);

        OffsetDateTime from = OffsetDateTime.now(UTC).minusDays(1);
        OffsetDateTime to = OffsetDateTime.now(UTC).plusDays(1);

        SensorDataDto entryDto = new SensorDataDto();
        entryDto.setDateTime(OffsetDateTime.now(UTC));
        entryDto.setTemperature(VALID_TEMPERATURE);
        entryDto.setHumidity(VALID_HUMIDITY);

        saveSensorData(user, entryDto);

        var response = webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/sensordata")
                        .queryParam("from", from.toString())
                        .queryParam("to", to.toString())
                        .build())
                .header("Authorization", "Bearer" + token)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(ResponseSensorDataDto.class)
                .returnResult().getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response).isNotEmpty();
        assertThat(response.get(0).getTemperature().compareTo(entryDto.getTemperature())).isEqualTo(0);
        assertThat(response.get(0).getHumidity().compareTo(entryDto.getHumidity())).isEqualTo(0);
        assertThat(response.get(0).getRespondentId()).isEqualTo(user.getId());
    }

    @Test
    void getDateOfLastSensorDataForRespondent_NonExistentRespondent_ShouldReturnBadRequest(){
        UUID nonExistentRespondentId = UUID.randomUUID();
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("api/sensordata/last")
                        .queryParam("respondentId", nonExistentRespondentId)
                        .build())
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void getDateOfLastSensorDataForRespondent_ValidRespondent_ShouldReturnOkStatus() {
        IdentityUser user1 = createUserWithRole("Respondent");

        SensorDataDto entryDto1 = new SensorDataDto();
        entryDto1.setDateTime(OffsetDateTime.now(UTC).minusDays(1));
        entryDto1.setTemperature(VALID_TEMPERATURE);
        entryDto1.setHumidity(VALID_HUMIDITY);
        saveSensorData(user1, entryDto1);

        SensorDataDto entryDto2 = new SensorDataDto();
        entryDto2.setDateTime(OffsetDateTime.now(UTC));
        entryDto2.setTemperature(VALID_TEMPERATURE.add(BigDecimal.ONE));
        entryDto2.setHumidity(VALID_HUMIDITY);
        saveSensorData(user1, entryDto2);

        var response = webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/sensordata/last")
                        .queryParam("respondentId", user1.getId())
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(LastSensorEntryDateDto.class)
                .returnResult().getResponseBody();

        assertThat(response).isNotNull();

        OffsetDateTime expected = entryDto2.getDateTime();
        OffsetDateTime actual = response.getDateTime();
        assertThat(actual).isBetween(expected.minusSeconds(1), expected.plusSeconds(1));
    }

    @Test
    void getLastSensorData_RespondentWithNoData_ShouldReturnNotFound() {
        IdentityUser user = createUserWithRole("Respondent");

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/sensordata/last")
                        .queryParam("respondentId", user.getId())
                        .build())
                .exchange()
                .expectStatus().isNotFound();
    }


    private IdentityUser createUserWithRole(String role){
        IdentityUser user = new IdentityUser()
                .setId(UUID.randomUUID())
                .setRole(role)
                .setUsername(UUID.randomUUID().toString())
                .setPasswordHash(passwordEncoder.encode("pswd"));

        return userRepository.saveAndFlush(user);
    }

    private String authenticateAndGenerateToken(IdentityUser user){
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getUsername(), "pswd"));
        return tokenProvider.generateToken(authentication);
    }

    private void saveSensorData(IdentityUser user, SensorDataDto entryDto){
        String token = authenticateAndGenerateToken(user);

        webTestClient.post()
                .uri("/api/sensordata")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(List.of(entryDto))
                .exchange()
                .expectStatus().isCreated();
    }

}

package com.survey.api.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.survey.api.security.TokenProvider;
import com.survey.application.dtos.*;
import com.survey.application.dtos.initialSurvey.InitialSurveyQuestionResponseDto;
import com.survey.application.dtos.surveyDtos.*;
import com.survey.domain.models.*;
import com.survey.domain.models.enums.QuestionType;
import com.survey.domain.models.enums.RespondentFilterOption;
import com.survey.domain.models.enums.Visibility;
import com.survey.domain.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;


@ExtendWith(IntegrationTestDatabaseInitializer.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "ADMIN_USER_PASSWORD=testAdminPassword")
@AutoConfigureWebTestClient()
public class RespondentDataControllerIntegrationTest {
    private final WebTestClient webTestClient;
    private final IdentityUserRepository userRepository;
    private final RespondentToGroupRepository respondentToGroupRepository;
    private final InitialSurveyRepository initialSurveyRepository;
    private final SurveyRepository surveyRepository;
    private final RespondentGroupRepository respondentGroupRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final RespondentDataRepository respondentDataRepository;
    private final AuthenticationManager authenticationManager;
    private static final String QUESTION_CONTENT = "What is your favorite color?";
    private static final int QUESTION_ORDER = 1;
    private static final String OPTION_CONTENT_1 = "Red";
    private static final String OPTION_CONTENT_2 = "Blue";
    private static final int OPTION_ORDER = 1;
    private static final String SECTION_NAME = "Section1";
    private static final String adminPassword = "testAdminPassword";

    @Autowired
    public RespondentDataControllerIntegrationTest(WebTestClient webTestClient, IdentityUserRepository userRepository, RespondentToGroupRepository respondentToGroupRepository, InitialSurveyRepository initialSurveyRepository, SurveyRepository surveyRepository, RespondentGroupRepository respondentGroupRepository, PasswordEncoder passwordEncoder,
                                                   TokenProvider tokenProvider, RespondentDataRepository respondentDataRepository, AuthenticationManager authenticationManager) {
        this.webTestClient = webTestClient;
        this.userRepository = userRepository;
        this.respondentToGroupRepository = respondentToGroupRepository;
        this.initialSurveyRepository = initialSurveyRepository;
        this.surveyRepository = surveyRepository;
        this.respondentGroupRepository = respondentGroupRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.respondentDataRepository = respondentDataRepository;
        this.authenticationManager = authenticationManager;
    }
    @BeforeEach
    void setUp() {
        respondentToGroupRepository.deleteAll();
        respondentDataRepository.deleteAll();
        userRepository.deleteAll();
        surveyRepository.deleteAll();
        initialSurveyRepository.deleteAll();
        List<RespondentGroup> groupsToDelete = respondentGroupRepository.findAll().stream()
                .filter(group -> !group.getName().equals("All"))
                .toList();
        respondentGroupRepository.deleteAll(groupsToDelete);
    }
    @Test
    void createRespondent_ShouldReturnCreatedResponse() {
        IdentityUser validRespondent = createIdentityUserForRespondent("respondent");
        String validRespondentToken = generateTokenForRespondent(validRespondent);

        List<InitialSurveyQuestionResponseDto> initialSurvey = saveInitialSurvey();
        CreateRespondentDataDto createRespondentDataDto = createRespondentDataDto(initialSurvey, 0);

        Map<String, Object> bodyRespondent = webTestClient.post()
                .uri("/api/respondents")
                .header("Authorization", "Bearer " + validRespondentToken)
                .bodyValue(Collections.singletonList(createRespondentDataDto))
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(new ParameterizedTypeReference<Map<String, Object>>() {})
                .returnResult()
                .getResponseBody();

        assertThat(bodyRespondent).isNotNull();
        assertThat(bodyRespondent).containsEntry("id", validRespondent.getId().toString());
        assertThat(bodyRespondent).containsEntry("username", validRespondent.getUsername());
        assertThat(bodyRespondent).containsEntry(QUESTION_CONTENT, initialSurvey.get(0).getOptions().get(0).getId().toString());
    }

    @Test
    void getFromUserContext_ShouldGiveNotFound_WhenTheRespondentDataWasNotCreatedYet(){
        IdentityUser respondent = createIdentityUserForRespondent("respondent");
        String token = generateTokenForRespondent(respondent);

        webTestClient.get().uri("/api/respondents")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    @Test
    void getAllForAdminShouldBeOk(){
        String adminToken = generateTokenForAdmin();

        webTestClient.get().uri("/api/respondents/all")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus()
                .isOk();
    }

    @Test
    void getFromUserContext_ShouldReturnUserRespondentData_WhenDataExists() {
        IdentityUser respondent = createIdentityUserForRespondent("respondent");
        String token = generateTokenForRespondent(respondent);

        List<InitialSurveyQuestionResponseDto> initialSurvey = saveInitialSurvey();
        saveInitialSurveyResponse(initialSurvey, 0, token);

        Map<String, Object> response = webTestClient.get().uri("/api/respondents")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<Map<String, Object>>() {})
                .returnResult().getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response).containsEntry("id", respondent.getId().toString());
        assertThat(response).containsEntry("username", respondent.getUsername());
        assertThat(response).containsEntry(QUESTION_CONTENT, initialSurvey.get(0).getOptions().get(0).getId().toString());
    }

    @Test
    void getAll_WithFilterOptionSkippedSurvey_ShouldReturnOk() throws JsonProcessingException {
        String adminToken = generateTokenForAdmin();
        IdentityUser validRespondent = createIdentityUserForRespondent("respondent1");
        String validRespondentToken = generateTokenForRespondent(validRespondent);
        IdentityUser invalidRespondent = createIdentityUserForRespondent("respondent2");

        ResponseSurveyDto survey = saveSurvey(createSurveyDto());
        saveSurveySendingPolicy(survey.getId());
        saveSurveyResponse(survey, validRespondentToken);

        OffsetDateTime from = OffsetDateTime.now(UTC).minusYears(1);
        OffsetDateTime to = OffsetDateTime.now(UTC).plusYears(1);

        List<Map<String, Object>> response = webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/respondents/all")
                        .queryParam("filterOption", RespondentFilterOption.skipped_surveys)
                        .queryParam("from", from.toString())
                        .queryParam("to", to.toString())
                        .queryParam("amount", 1)
                        .build())
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                .returnResult().getResponseBody();

        assertThat(response).isNotEmpty();
        assertThat(response).hasSize(1);
        assertThat(response.get(0)).containsEntry("id", invalidRespondent.getId().toString());
        assertThat(response.get(0)).containsEntry("username", invalidRespondent.getUsername());
    }

    @Test
    void getAll_WithFilterOptionSkippedSurvey_ForGroupSpecificSurveySection_ShouldReturnOk() throws JsonProcessingException {
        String adminToken = generateTokenForAdmin();
        IdentityUser validRespondent = createIdentityUserForRespondent("respondent1");
        String validRespondentToken = generateTokenForRespondent(validRespondent);
        IdentityUser invalidRespondent = createIdentityUserForRespondent("respondent2");
        String invalidRespondentToken = generateTokenForRespondent(invalidRespondent);

        List<InitialSurveyQuestionResponseDto> initialSurvey = saveInitialSurvey();
        saveInitialSurveyResponse(initialSurvey, 0, validRespondentToken);
        saveInitialSurveyResponse(initialSurvey, 1, invalidRespondentToken);

        String groupId = respondentGroupRepository.findByGroupName(QUESTION_CONTENT + " - " + OPTION_CONTENT_2).getId().toString();
        ResponseSurveyDto survey = saveSurvey(createSurveyGroupSpecificDto(groupId));
        saveSurveySendingPolicy(survey.getId());

        OffsetDateTime from = OffsetDateTime.now(UTC).minusYears(1);
        OffsetDateTime to = OffsetDateTime.now(UTC).plusYears(1);

        List<Map<String, Object>> response = webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/respondents/all")
                        .queryParam("filterOption", RespondentFilterOption.skipped_surveys)
                        .queryParam("from", from.toString())
                        .queryParam("to", to.toString())
                        .queryParam("amount", 1)
                        .build())
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                .returnResult().getResponseBody();

        assertThat(response).isNotEmpty();
        assertThat(response).hasSize(1);
        assertThat(response.get(0)).containsEntry("id", invalidRespondent.getId().toString());
        assertThat(response.get(0)).containsEntry("username", invalidRespondent.getUsername());
        assertThat(response.get(0)).containsEntry(QUESTION_CONTENT, initialSurvey.get(0).getOptions().get(1).getId().toString());
    }

    @Test
    void getAll_WithFilterOptionLocationNotSent_ShouldReturnOk() {
        String adminToken = generateTokenForAdmin();
        IdentityUser validRespondent = createIdentityUserForRespondent("respondent1");
        String validRespondentToken = generateTokenForRespondent(validRespondent);
        IdentityUser invalidRespondent = createIdentityUserForRespondent("respondent2");

        saveLocalizationDataForRespondent(validRespondentToken, OffsetDateTime.now());
        saveLocalizationDataForRespondent(validRespondentToken, OffsetDateTime.now().minusDays(1));

        OffsetDateTime from = OffsetDateTime.now(UTC).minusYears(1);
        OffsetDateTime to = OffsetDateTime.now(UTC).plusYears(1);

        List<Map<String, Object>> response = webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/respondents/all")
                        .queryParam("filterOption", RespondentFilterOption.location_not_sent)
                        .queryParam("from", from.toString())
                        .queryParam("to", to.toString())
                        .queryParam("amount", 1)
                        .build())
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                .returnResult().getResponseBody();

        assertThat(response).isNotEmpty();
        assertThat(response).hasSize(1);
        assertThat(response.get(0)).containsEntry("id", invalidRespondent.getId().toString());
        assertThat(response.get(0)).containsEntry("username", invalidRespondent.getUsername());
    }

    @Test
    void getAll_WithFilterOptionSensorDataNotSent_ShouldReturnOk() {
        String adminToken = generateTokenForAdmin();
        IdentityUser validRespondent = createIdentityUserForRespondent("respondent1");
        String validRespondentToken = generateTokenForRespondent(validRespondent);
        IdentityUser invalidRespondent = createIdentityUserForRespondent("respondent2");

        saveSensorDataForRespondent(validRespondentToken, OffsetDateTime.now());
        saveSensorDataForRespondent(validRespondentToken, OffsetDateTime.now().minusDays(2));

        OffsetDateTime from = OffsetDateTime.now(UTC).minusYears(1);
        OffsetDateTime to = OffsetDateTime.now(UTC).plusYears(1);

        List<Map<String, Object>> response = webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/respondents/all")
                        .queryParam("filterOption", RespondentFilterOption.sensors_data_not_sent)
                        .queryParam("from", from.toString())
                        .queryParam("to", to.toString())
                        .queryParam("amount", 1)
                        .build())
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                .returnResult().getResponseBody();

        assertThat(response).isNotEmpty();
        assertThat(response).hasSize(1);
        assertThat(response.get(0)).containsEntry("id", invalidRespondent.getId().toString());
        assertThat(response.get(0)).containsEntry("username", invalidRespondent.getUsername());
    }

    private void saveSensorDataForRespondent(String token, OffsetDateTime time) {
        SensorDataDto entryDto = new SensorDataDto();
        entryDto.setDateTime(time);
        entryDto.setTemperature(new BigDecimal("21.5"));
        entryDto.setHumidity(new BigDecimal("51.5"));

        webTestClient.post()
                .uri("/api/sensordata")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(List.of(entryDto))
                .exchange()
                .expectStatus().isCreated();
    }
    private void saveLocalizationDataForRespondent(String token, OffsetDateTime time){
        LocalizationDataDto localizationDataDto = new LocalizationDataDto();
        localizationDataDto.setLatitude(new BigDecimal("52.237049"));
        localizationDataDto.setLongitude(new BigDecimal("22.237049"));
        localizationDataDto.setDateTime(time);

        webTestClient.post()
                .uri("/api/localization")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(List.of(localizationDataDto))
                .exchange()
                .expectStatus().isCreated();
    }

    private List<InitialSurveyQuestionResponseDto> saveInitialSurvey() {
        InitialSurveyOption option1 = new InitialSurveyOption();
        option1.setContent(OPTION_CONTENT_1);
        option1.setOrder(1);

        InitialSurveyOption option2 = new InitialSurveyOption();
        option2.setContent(OPTION_CONTENT_2);
        option2.setOrder(2);

        InitialSurveyQuestion question = new InitialSurveyQuestion();
        question.setContent(QUESTION_CONTENT);
        question.setOrder(QUESTION_ORDER);
        question.setOptions(List.of(option1, option2));

        List<InitialSurveyQuestionResponseDto> initialSurvey =  webTestClient.post()
                .uri("/api/initialsurvey")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Collections.singletonList(question))
                .exchange()
                .expectStatus().isCreated()
                .expectBodyList(InitialSurveyQuestionResponseDto.class)
                .returnResult()
                .getResponseBody();

        webTestClient.patch()
                .uri("/api/initialsurvey/publish")
                .exchange()
                .expectStatus().isNoContent();

        return initialSurvey;
    }
    private CreateRespondentDataDto createRespondentDataDto(List<InitialSurveyQuestionResponseDto> initialSurvey, Integer optionId){
        CreateRespondentDataDto createRespondentDataDto = new CreateRespondentDataDto();
        createRespondentDataDto.setQuestionId(initialSurvey.get(0).getId());
        createRespondentDataDto.setOptionId(initialSurvey.get(0).getOptions().get(optionId).getId());
        return createRespondentDataDto;
    }
    private void saveInitialSurveyResponse(List<InitialSurveyQuestionResponseDto> initialSurvey, Integer optionId, String token){
        webTestClient.post()
                .uri("/api/respondents")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Collections.singletonList(createRespondentDataDto(initialSurvey, optionId)))
                .exchange()
                .expectStatus().isCreated();
    }
    private ResponseSurveyDto saveSurvey(CreateSurveyDto createSurveyDto) throws JsonProcessingException {
        String jsonSurveyDto = new ObjectMapper().writeValueAsString(createSurveyDto);

        MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();
        multipartBodyBuilder.part("json", jsonSurveyDto);

        return webTestClient.post()
                .uri("/api/surveys")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(multipartBodyBuilder.build()))
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(ResponseSurveyDto.class)
                .returnResult()
                .getResponseBody();
    }
    private void saveSurveySendingPolicy(UUID surveyId) {
        SurveyParticipationTimeStartFinishDto participationTimeSlot = new SurveyParticipationTimeStartFinishDto();
        participationTimeSlot.setStart(OffsetDateTime.now(UTC).minusMonths(1));
        participationTimeSlot.setFinish(OffsetDateTime.now(UTC).plusMonths(1));

        CreateSurveySendingPolicyDto createSurveySendingPolicyDto = new CreateSurveySendingPolicyDto();
        createSurveySendingPolicyDto.setSurveyId(surveyId);
        createSurveySendingPolicyDto.setSurveyParticipationTimeSlots(List.of(participationTimeSlot));

        webTestClient.post()
                .uri("/api/surveysendingpolicies")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createSurveySendingPolicyDto)
                .exchange()
                .expectStatus()
                .isCreated();
    }

    private void saveSurveyResponse(ResponseSurveyDto surveyDto, String token){
        SelectedOptionDto selectedOptionDto = new SelectedOptionDto();
        selectedOptionDto.setOptionId(surveyDto.getSections().get(0).getQuestions().get(0).getOptions().get(0).getId());

        AnswerDto answerDto = new AnswerDto();
        answerDto.setQuestionId(surveyDto.getSections().get(0).getQuestions().get(0).getId());
        answerDto.setSelectedOptions(List.of(selectedOptionDto));

        SendOnlineSurveyResponseDto sendSurveyResponseDto = new SendOnlineSurveyResponseDto();
        sendSurveyResponseDto.setSurveyId(surveyDto.getId());
        sendSurveyResponseDto.setAnswers(List.of(answerDto));
        sendSurveyResponseDto.setStartDate(OffsetDateTime.now().minusHours(1));
        sendSurveyResponseDto.setFinishDate(OffsetDateTime.now());

        webTestClient.post()
                .uri("/api/surveyresponses")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(sendSurveyResponseDto)
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(SurveyParticipationDto.class)
                .returnResult()
                .getResponseBody();
    }
    private CreateSurveyDto createSurveyDto(){
        CreateOptionDto createOptionDto = new CreateOptionDto();
        createOptionDto.setLabel(OPTION_CONTENT_1);
        createOptionDto.setOrder(OPTION_ORDER);
        createOptionDto.setImagePath(null);

        CreateQuestionDto createQuestionDto = new CreateQuestionDto();
        createQuestionDto.setQuestionType(QuestionType.single_choice.name());
        createQuestionDto.setOrder(QUESTION_ORDER);
        createQuestionDto.setContent(QUESTION_CONTENT);
        createQuestionDto.setOptions(List.of(createOptionDto));

        CreateSurveySectionDto createSurveySectionDto = new CreateSurveySectionDto();
        createSurveySectionDto.setName(SECTION_NAME);
        createSurveySectionDto.setOrder(1);
        createSurveySectionDto.setDisplayOnOneScreen(true);
        createSurveySectionDto.setVisibility(Visibility.always.name());
        createSurveySectionDto.setQuestions(List.of(createQuestionDto));

        CreateSurveyDto createSurveyDto = new CreateSurveyDto();
        createSurveyDto.setName("Survey");
        createSurveyDto.setSections(List.of(createSurveySectionDto));
        return createSurveyDto;
    }
    private CreateSurveyDto createSurveyGroupSpecificDto(String groupId){
        CreateOptionDto createOptionDto = new CreateOptionDto();
        createOptionDto.setLabel(OPTION_CONTENT_1);
        createOptionDto.setOrder(OPTION_ORDER);
        createOptionDto.setImagePath(null);

        CreateQuestionDto createQuestionDto = new CreateQuestionDto();
        createQuestionDto.setQuestionType(QuestionType.single_choice.name());
        createQuestionDto.setOrder(QUESTION_ORDER);
        createQuestionDto.setContent(QUESTION_CONTENT);
        createQuestionDto.setOptions(List.of(createOptionDto));

        CreateSurveySectionDto createSurveySectionDto = new CreateSurveySectionDto();
        createSurveySectionDto.setName(SECTION_NAME);
        createSurveySectionDto.setOrder(1);
        createSurveySectionDto.setDisplayOnOneScreen(true);
        createSurveySectionDto.setVisibility(Visibility.group_specific.name());
        createSurveySectionDto.setGroupId(groupId);
        createSurveySectionDto.setQuestions(List.of(createQuestionDto));

        CreateSurveyDto createSurveyDto = new CreateSurveyDto();
        createSurveyDto.setName("Survey Group Specific");
        createSurveyDto.setSections(List.of(createSurveySectionDto));
        return createSurveyDto;
    }
    private String generateTokenForAdmin() {
        IdentityUser identityUser = new IdentityUser()
                .setId(UUID.randomUUID())
                .setRole("ADMIN")
                .setUsername(UUID.randomUUID().toString())
                .setPasswordHash(passwordEncoder.encode(adminPassword));

        identityUser = userRepository.saveAndFlush(identityUser);

        Authentication authentication = authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(identityUser.getUsername(),
                        adminPassword));

        return tokenProvider.generateToken(authentication);
    }
    private IdentityUser createIdentityUserForRespondent(String username){
        IdentityUser identityUser = new IdentityUser()
                .setRole("Respondent")
                .setUsername(username)
                .setPasswordHash(passwordEncoder.encode("password"));

        identityUser = userRepository.saveAndFlush(identityUser);
        return identityUser;
    }
    private String generateTokenForRespondent(IdentityUser identityUser) {
        Authentication authentication = authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(identityUser.getUsername(),
                        "password"));

        return tokenProvider.generateToken(authentication);
    }
}
package com.survey.api.integration;

import com.survey.api.TestUtils;
import com.survey.api.security.Role;
import com.survey.application.dtos.*;
import com.survey.application.dtos.surveyDtos.*;
import com.survey.domain.models.IdentityUser;
import com.survey.domain.models.enums.QuestionType;
import com.survey.domain.models.enums.Visibility;
import com.survey.domain.repository.IdentityUserRepository;
import com.survey.domain.repository.SurveyParticipationRepository;
import com.survey.domain.repository.SurveyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(IntegrationTestDatabaseInitializer.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "ADMIN_USER_PASSWORD=testAdminPassword")
@AutoConfigureWebTestClient
public class SurveyResponsesControllerIntegrationTest {
    private final WebTestClient webTestClient;
    private final IdentityUserRepository userRepository;
    private final SurveyRepository surveyRepository;
    private final SurveyParticipationRepository surveyParticipationRepository;
    private final TestUtils testUtils;
    private static final String QUESTION_CONTENT = "What is your favorite color?";
    private static final int QUESTION_ORDER = 1;
    private static final String OPTION_CONTENT_1 = "Red";
    private static final String OPTION_CONTENT_2 = "Blue";
    private static final int OPTION_ORDER_1 = 1;
    private static final int OPTION_ORDER_2 = 2;
    private static final String SURVEY_NAME_1 = "Survey1";
    private static final String SURVEY_NAME_2 = "Survey2";
    private static final String SECTION_NAME = "Section1";
    private static final String ADMIN_PASSWORD = "testAdminPassword";
    private static final String RESPONDENT_PASSWORD_1 = "testRespondentPassword1";
    private static final String RESPONDENT_PASSWORD_2 = "testRespondentPassword2";

    @Autowired
    public SurveyResponsesControllerIntegrationTest(WebTestClient webTestClient,
                                           IdentityUserRepository userRepository,
                                           SurveyRepository surveyRepository,
                                           SurveyParticipationRepository surveyParticipationRepository, TestUtils testUtils) {
        this.webTestClient = webTestClient;
        this.userRepository = userRepository;
        this.surveyRepository = surveyRepository;
        this.surveyParticipationRepository = surveyParticipationRepository;
        this.testUtils = testUtils;
    }
    @BeforeEach
    void SetUp(){
        surveyParticipationRepository.deleteAll();
        userRepository.deleteAll();
        surveyRepository.deleteAll();
    }
    @Test
    void getResponsesResults_shouldReturnAllResults_WhenNoParams() {
        IdentityUser admin = testUtils.createUserWithRole(Role.ADMIN.getRoleName(), ADMIN_PASSWORD);
        String adminToken = testUtils.authenticateAndGenerateToken(admin, ADMIN_PASSWORD);
        IdentityUser respondent = testUtils.createUserWithRole(Role.RESPONDENT.getRoleName(), RESPONDENT_PASSWORD_1);
        String respondentToken = testUtils.authenticateAndGenerateToken(respondent, RESPONDENT_PASSWORD_1);

        ResponseSurveyDto survey = saveSurvey(createSurveyDto(SURVEY_NAME_1));
        saveSurveySendingPolicy(survey.getId());
        saveSurveyResponse(survey, respondentToken);

        var response = webTestClient.get()
                .uri("/api/surveyresponses/results")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(SurveyResultDto.class)
                .returnResult().getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response).hasSize(1);
        assertThat(response.get(0).getRespondentId()).isEqualTo(respondent.getId());
        assertThat(response.get(0).getSurveyName()).isEqualTo(SURVEY_NAME_1);
    }

    @Test
    void getResponsesResults_shouldReturnResults_WithParamSurveyId() {
        IdentityUser admin = testUtils.createUserWithRole(Role.ADMIN.getRoleName(), ADMIN_PASSWORD);
        String adminToken = testUtils.authenticateAndGenerateToken(admin, ADMIN_PASSWORD);
        IdentityUser respondent = testUtils.createUserWithRole(Role.RESPONDENT.getRoleName(), RESPONDENT_PASSWORD_1);
        String respondentToken = testUtils.authenticateAndGenerateToken(respondent, RESPONDENT_PASSWORD_1);

        ResponseSurveyDto survey1 = saveSurvey(createSurveyDto(SURVEY_NAME_1));
        saveSurveySendingPolicy(survey1.getId());
        saveSurveyResponse(survey1, respondentToken);

        ResponseSurveyDto survey2 = saveSurvey(createSurveyDto(SURVEY_NAME_2));
        saveSurveySendingPolicy(survey2.getId());
        saveSurveyResponse(survey2, respondentToken);

        var response = webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/surveyresponses/results")
                        .queryParam("surveyId", survey1.getId())
                        .build())
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(SurveyResultDto.class)
                .returnResult().getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response).hasSize(1);
        assertThat(response.get(0).getRespondentId()).isEqualTo(respondent.getId());
        assertThat(response.get(0).getSurveyName()).isEqualTo(SURVEY_NAME_1);
    }

    @Test
    void getResponsesResults_shouldReturnResults_WithParamRespondentId() {
        IdentityUser admin = testUtils.createUserWithRole(Role.ADMIN.getRoleName(), ADMIN_PASSWORD);
        String adminToken = testUtils.authenticateAndGenerateToken(admin, ADMIN_PASSWORD);

        IdentityUser respondent1 = testUtils.createUserWithRole(Role.RESPONDENT.getRoleName(), RESPONDENT_PASSWORD_1);
        String respondentToken1 = testUtils.authenticateAndGenerateToken(respondent1, RESPONDENT_PASSWORD_1);

        IdentityUser respondent2 = testUtils.createUserWithRole(Role.RESPONDENT.getRoleName(), RESPONDENT_PASSWORD_2);
        String respondentToken2 = testUtils.authenticateAndGenerateToken(respondent2, RESPONDENT_PASSWORD_2);

        ResponseSurveyDto survey1 = saveSurvey(createSurveyDto(SURVEY_NAME_1));
        saveSurveySendingPolicy(survey1.getId());
        saveSurveyResponse(survey1, respondentToken1);
        saveSurveyResponse(survey1, respondentToken2);

        var response = webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/surveyresponses/results")
                        .queryParam("respondentId", respondent1.getId())
                        .build())
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(SurveyResultDto.class)
                .returnResult().getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response).hasSize(1);
        assertThat(response.get(0).getRespondentId()).isEqualTo(respondent1.getId());
        assertThat(response.get(0).getSurveyName()).isEqualTo(SURVEY_NAME_1);
    }

    @Test
    void getResponsesResults_shouldReturnCorrectResult_WithAllParams() {
        IdentityUser admin = testUtils.createUserWithRole(Role.ADMIN.getRoleName(), ADMIN_PASSWORD);
        String adminToken = testUtils.authenticateAndGenerateToken(admin, ADMIN_PASSWORD);
        IdentityUser respondent = testUtils.createUserWithRole(Role.RESPONDENT.getRoleName(), RESPONDENT_PASSWORD_1);
        String respondentToken = testUtils.authenticateAndGenerateToken(respondent, RESPONDENT_PASSWORD_1);

        ResponseSurveyDto survey = saveSurvey(createSurveyDto(SURVEY_NAME_1));
        saveSurveySendingPolicy(survey.getId());
        saveSurveyResponse(survey, respondentToken);

        OffsetDateTime from = OffsetDateTime.now(UTC).minusYears(1);
        OffsetDateTime to = OffsetDateTime.now(UTC).plusYears(1);

        var response = webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/surveyresponses/results")
                        .queryParam("surveyId", survey.getId())
                        .queryParam("respondentId", respondent.getId())
                        .queryParam("dateFrom", from.toString())
                        .queryParam("dateTo", to.toString())
                        .build())
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(SurveyResultDto.class)
                .returnResult().getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response).hasSize(1);
        assertThat(response.get(0).getRespondentId()).isEqualTo(respondent.getId());
        assertThat(response.get(0).getSurveyName()).isEqualTo(SURVEY_NAME_1);
    }


    private ResponseSurveyDto saveSurvey(CreateSurveyDto createSurveyDto) {
        IdentityUser admin = testUtils.createUserWithRole(Role.ADMIN.getRoleName(), ADMIN_PASSWORD);
        String adminToken = testUtils.authenticateAndGenerateToken(admin, ADMIN_PASSWORD);

        MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();
        multipartBodyBuilder.part("json", createSurveyDto, MediaType.APPLICATION_JSON);

        return webTestClient.post()
                .uri("/api/surveys")
                .header("Authorization", "Bearer " + adminToken)
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
        IdentityUser admin = testUtils.createUserWithRole(Role.ADMIN.getRoleName(), ADMIN_PASSWORD);
        String adminToken = testUtils.authenticateAndGenerateToken(admin, ADMIN_PASSWORD);

        SurveyParticipationTimeStartFinishDto participationTimeSlot = new SurveyParticipationTimeStartFinishDto();
        participationTimeSlot.setStart(OffsetDateTime.now(UTC).minusMonths(1));
        participationTimeSlot.setFinish(OffsetDateTime.now(UTC).plusMonths(1));

        CreateSurveySendingPolicyDto createSurveySendingPolicyDto = new CreateSurveySendingPolicyDto();
        createSurveySendingPolicyDto.setSurveyId(surveyId);
        createSurveySendingPolicyDto.setSurveyParticipationTimeSlots(List.of(participationTimeSlot));

        webTestClient.post()
                .uri("/api/surveysendingpolicies")
                .header("Authorization", "Bearer " + adminToken)
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

    private CreateSurveyDto createSurveyDto(String surveyName){
        CreateOptionDto createOptionDto1 = new CreateOptionDto();
        createOptionDto1.setLabel(OPTION_CONTENT_1);
        createOptionDto1.setOrder(OPTION_ORDER_1);
        createOptionDto1.setImagePath(null);

        CreateOptionDto createOptionDto2 = new CreateOptionDto();
        createOptionDto2.setLabel(OPTION_CONTENT_2);
        createOptionDto2.setOrder(OPTION_ORDER_2);
        createOptionDto2.setImagePath(null);

        CreateQuestionDto createQuestionDto = new CreateQuestionDto();
        createQuestionDto.setQuestionType(QuestionType.single_choice.name());
        createQuestionDto.setOrder(QUESTION_ORDER);
        createQuestionDto.setContent(QUESTION_CONTENT);
        createQuestionDto.setOptions(List.of(createOptionDto1, createOptionDto2));

        CreateSurveySectionDto createSurveySectionDto = new CreateSurveySectionDto();
        createSurveySectionDto.setName(SECTION_NAME);
        createSurveySectionDto.setOrder(1);
        createSurveySectionDto.setDisplayOnOneScreen(true);
        createSurveySectionDto.setVisibility(Visibility.always.name());
        createSurveySectionDto.setQuestions(List.of(createQuestionDto));

        CreateSurveyDto createSurveyDto = new CreateSurveyDto();
        createSurveyDto.setName(surveyName);
        createSurveyDto.setSections(List.of(createSurveySectionDto));
        return createSurveyDto;
    }

}

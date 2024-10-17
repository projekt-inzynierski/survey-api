package com.survey.api.validation;

import com.survey.application.dtos.RespondentGroupDto;
import com.survey.application.dtos.SurveySendingPolicyDto;
import com.survey.application.dtos.SurveySendingPolicyTimesDto;
import com.survey.application.dtos.surveyDtos.AnswerDto;
import com.survey.application.dtos.surveyDtos.SelectedOptionDto;
import com.survey.application.dtos.surveyDtos.SendSurveyResponseDto;
import com.survey.application.services.ClaimsPrincipalService;
import com.survey.application.services.RespondentGroupService;
import com.survey.application.services.SurveySendingPolicyService;
import com.survey.domain.models.*;
import com.survey.domain.models.enums.QuestionType;
import com.survey.domain.models.enums.Visibility;
import com.survey.domain.repository.OptionRepository;
import com.survey.domain.repository.RespondentDataRepository;
import com.survey.domain.repository.SurveyRepository;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SendSurveyResponseDtoValidatorTest {

    @Mock
    private SurveyRepository surveyRepository;
    @Mock
    private SurveySendingPolicyService surveySendingPolicyService;
    @Mock
    private ConstraintValidatorContext context;
    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder;
    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext nodeBuilderCustomizableContext;
    @Mock
    private ClaimsPrincipalService claimsPrincipalService;
    @Mock
    private RespondentDataRepository respondentDataRepository;
    @Mock
    private RespondentGroupService respondentGroupService;
    @Mock
    private OptionRepository optionRepository;

    @InjectMocks
    private SendSurveyResponseDtoValidator validator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);
        when(violationBuilder.addPropertyNode(anyString())).thenReturn(nodeBuilderCustomizableContext);
        when(nodeBuilderCustomizableContext.addConstraintViolation()).thenReturn(context);
    }

    @Test
    void shouldFailWhenSurveyDoesNotExist() {
        UUID surveyId = UUID.randomUUID();
        SendSurveyResponseDto dto = new SendSurveyResponseDto();
        dto.setSurveyId(surveyId);
        dto.setAnswers(Collections.emptyList());

        boolean isValid = validator.isValid(dto, context);

        assertFalse(isValid);
        verify(context).buildConstraintViolationWithTemplate("This survey does not exist");
    }

    @Test
    void shouldFailWhenAnswerDoesNotMatchExistingQuestion() {
        UUID surveyId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        Survey survey = new Survey();
        survey.setSections(Collections.singletonList(new SurveySection()));
        when(surveyRepository.findById(surveyId)).thenReturn(Optional.of(survey));
        when(surveySendingPolicyService.getSurveysSendingPolicyById(survey.getId()))
                .thenReturn(List.of(validSurveySendingPolicy(survey.getId())));
        AnswerDto answerDto = new AnswerDto();
        answerDto.setQuestionId(questionId);

        SendSurveyResponseDto dto = new SendSurveyResponseDto();
        dto.setSurveyId(surveyId);
        dto.setAnswers(Collections.singletonList(answerDto));

        boolean isValid = validator.isValid(dto, context);

        assertFalse(isValid);
    }

    @ParameterizedTest
    @MethodSource("getValidDataWithSingleQuestionSurvey")
    void shouldPassWithAnswerMatchingQuestionType(Survey survey, SendSurveyResponseDto response) {
        when(surveyRepository.findById(survey.getId())).thenReturn(Optional.of(survey));
        when(surveySendingPolicyService.getSurveysSendingPolicyById(survey.getId()))
                .thenReturn(List.of(validSurveySendingPolicy(survey.getId())));
        boolean isValid = validator.isValid(response, context);
        assertTrue(isValid);
    }

    public static Stream<Arguments> getValidDataWithSingleQuestionSurvey(){
        UUID questionId = UUID.randomUUID();
        UUID optionId = UUID.randomUUID();
        return Stream.of(
                getArgumentsWithSingleQuestionSurvey(
                        new Question(
                                questionId,
                                null,
                                null,
                                null,
                                QuestionType.yes_no_selection,
                                true,
                                null,
                                null,
                                null
                        ),
                        new AnswerDto(questionId, null, null, true)
                ),
                getArgumentsWithSingleQuestionSurvey(
                        new Question(
                                questionId,
                                null,
                                null,
                                null,
                                QuestionType.discrete_number_selection,
                                true,
                                null,
                                null,
                                new NumberRange(
                                        UUID.randomUUID(),
                                        1,
                                        5,
                                        null, null,
                                        null, null
                                )
                        ),
                        new AnswerDto(questionId, null, 3, null)
                ),
                getArgumentsWithSingleQuestionSurvey(
                        new Question(
                                questionId,
                                null,
                                null,
                                null,
                                QuestionType.single_text_selection,
                                true,
                                null,
                                Stream.of(
                                        new Option(
                                                optionId,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null
                                        )
                                ).collect(Collectors.toList()),
                                null
                        ),
                        new AnswerDto(questionId, Stream.of(new SelectedOptionDto(optionId)).collect(Collectors.toList()), null, null)
                ),
                getArgumentsWithSingleQuestionSurvey(
                        new Question(
                                questionId,
                                null,
                                null,
                                null,
                                QuestionType.number_selection,
                                true,
                                null,
                                null,
                                null
                        ),
                        new AnswerDto(questionId, null, new Random().nextInt(), null)
                )
        );
    }

    private static Arguments getArgumentsWithSingleQuestionSurvey(Question question, AnswerDto answerDto){
        UUID surveyId = UUID.randomUUID();
        return  Arguments.of(
                new Survey(
                        surveyId,
                        "name",
                        null,
                        Collections.singletonList(
                                new SurveySection(
                                        UUID.randomUUID(),
                                        null,
                                        null,
                                        null,
                                        Visibility.always,
                                        null,
                                        Stream.of(
                                                question
                                        ).collect(Collectors.toList()),
                                        null
                                )
                        ),
                        null
                ),
                new SendSurveyResponseDto(
                        surveyId,
                        Stream.of(
                                answerDto
                        ).collect(Collectors.toList())
                )
        );
    }

    @ParameterizedTest
    @MethodSource("getInvalidDataWithSingleQuestion")
    void shouldFailWhenSingleAnswerIsInvalidOrMissing(Survey survey, SendSurveyResponseDto response) {
        when(surveyRepository.findById(survey.getId())).thenReturn(Optional.of(survey));
        when(surveySendingPolicyService.getSurveysSendingPolicyById(survey.getId()))
                .thenReturn(List.of(validSurveySendingPolicy(survey.getId())));
        boolean isValid = validator.isValid(response, context);
        assertFalse(isValid);
    }

    public static Stream<Arguments> getInvalidDataWithSingleQuestion(){
        UUID questionId = UUID.randomUUID();
        UUID optionId = UUID.randomUUID();
        List<SelectedOptionDto> optionList = Stream.of(new SelectedOptionDto(UUID.randomUUID())).collect(Collectors.toList());
        List<SelectedOptionDto> optionListOfMoreThanOneSelectedOptions = Stream.of(
                new SelectedOptionDto(UUID.randomUUID()),
                new SelectedOptionDto(UUID.randomUUID())
        ).toList();
        return Stream.of(
                getArgumentsWithSingleQuestionSurvey(
                        new Question(
                                questionId,
                                null,
                                null,
                                null,
                                QuestionType.yes_no_selection,
                                true,
                                null,
                                null,
                                null
                        ),
                        new AnswerDto(questionId, null, null, null)
                ),
                getArgumentsWithSingleQuestionSurvey(
                        new Question(
                                questionId,
                                null,
                                null,
                                null,
                                QuestionType.yes_no_selection,
                                true,
                                null,
                                null,
                                null
                        ),
                        new AnswerDto(questionId, null, 1, null)
                ),
                getArgumentsWithSingleQuestionSurvey(
                        new Question(
                                questionId,
                                null,
                                null,
                                null,
                                QuestionType.yes_no_selection,
                                true,
                                null,
                                null,
                                null
                        ),
                        new AnswerDto(questionId, optionList, 1, null)
                ),
                getArgumentsWithSingleQuestionSurvey(
                        new Question(
                                questionId,
                                null,
                                null,
                                null,
                                QuestionType.discrete_number_selection,
                                true,
                                null,
                                null,
                                new NumberRange(
                                        UUID.randomUUID(),
                                        1,
                                        5,
                                        null, null,
                                        null, null
                                )
                        ),
                        new AnswerDto(questionId, null, 6, null)
                ),
                getArgumentsWithSingleQuestionSurvey(
                        new Question(
                                questionId,
                                null,
                                null,
                                null,
                                QuestionType.discrete_number_selection,
                                true,
                                null,
                                null,
                                new NumberRange(
                                        UUID.randomUUID(),
                                        1,
                                        5,
                                        null, null,
                                        null, null
                                )
                        ),
                        new AnswerDto(questionId, null, 0, null)
                ),
                getArgumentsWithSingleQuestionSurvey(
                        new Question(
                                questionId,
                                null,
                                null,
                                null,
                                QuestionType.discrete_number_selection,
                                true,
                                null,
                                null,
                                new NumberRange(
                                        UUID.randomUUID(),
                                        1,
                                        5,
                                        null, null,
                                        null, null
                                )
                        ),
                        new AnswerDto(questionId, null, null, null)
                ),
                getArgumentsWithSingleQuestionSurvey(
                        new Question(
                                questionId,
                                null,
                                null,
                                null,
                                QuestionType.discrete_number_selection,
                                true,
                                null,
                                null,
                                new NumberRange(
                                        UUID.randomUUID(),
                                        1,
                                        5,
                                        null, null,
                                        null, null
                                )
                        ),
                        new AnswerDto(questionId, optionList, null, null)
                ),
                getArgumentsWithSingleQuestionSurvey(
                        new Question(
                                questionId,
                                null,
                                null,
                                null,
                                QuestionType.discrete_number_selection,
                                true,
                                null,
                                null,
                                new NumberRange(
                                        UUID.randomUUID(),
                                        1,
                                        5,
                                        null, null,
                                        null, null
                                )
                        ),
                        new AnswerDto(questionId, null, null, Boolean.FALSE)
                ),
                getArgumentsWithSingleQuestionSurvey(
                        new Question(
                                questionId,
                                null,
                                null,
                                null,
                                QuestionType.single_text_selection,
                                true,
                                null,
                                Stream.of(
                                        new Option(
                                                optionId,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null
                                        )
                                ).collect(Collectors.toList()),
                                null
                        ),
                        new AnswerDto(questionId, optionList, null, null)
                ),
                getArgumentsWithSingleQuestionSurvey(
                        new Question(
                                questionId,
                                null,
                                null,
                                null,
                                QuestionType.single_text_selection,
                                true,
                                null,
                                Stream.of(
                                        new Option(
                                                optionId,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null
                                        )
                                ).collect(Collectors.toList()),
                                null
                        ),
                        new AnswerDto(questionId, optionList, 1, null)
                ),
                getArgumentsWithSingleQuestionSurvey(
                        new Question(
                                questionId,
                                null,
                                null,
                                null,
                                QuestionType.single_text_selection,
                                true,
                                null,
                                Stream.of(
                                        new Option(
                                                optionId,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null
                                        )
                                ).collect(Collectors.toList()),
                                null
                        ),
                        new AnswerDto(questionId, optionList, null, Boolean.FALSE)
                ),
                getArgumentsWithSingleQuestionSurvey(
                        new Question(
                                questionId,
                                null,
                                null,
                                null,
                                QuestionType.single_text_selection,
                                true,
                                null,
                                Stream.of(
                                        new Option(
                                                optionId,
                                                null,
                                                null,
                                                null,
                                                null,
                                                null
                                        )
                                ).collect(Collectors.toList()),
                                null
                        ),
                        new AnswerDto(questionId, optionListOfMoreThanOneSelectedOptions, null, null)
                ),
                getArgumentsWithSingleQuestionSurvey(
                        new Question(
                                questionId,
                                null,
                                null,
                                null,
                                QuestionType.number_selection,
                                true,
                                null,
                                null,
                                null
                        ),
                        new AnswerDto(questionId, null, null, true)
                ),
                getArgumentsWithSingleQuestionSurvey(
                        new Question(
                                questionId,
                                null,
                                null,
                                null,
                                QuestionType.number_selection,
                                true,
                                null,
                                null,
                                null
                        ),
                        new AnswerDto(questionId, optionList, null, null)
                ),
                getArgumentsWithSingleQuestionSurvey(
                        new Question(
                                questionId,
                                null,
                                null,
                                null,
                                QuestionType.number_selection,
                                true,
                                null,
                                null,
                                null
                        ),
                        new AnswerDto(questionId, null, null, null)
                )
        );
    }

    @Test
    void shouldFailWhenSurveyDoesNotHaveSendingPolicy() {
        UUID surveyId = UUID.randomUUID();
        SendSurveyResponseDto sendSurveyResponseDto = new SendSurveyResponseDto();
        sendSurveyResponseDto.setSurveyId(surveyId);
        sendSurveyResponseDto.setAnswers(List.of());

        when(surveyRepository.findById(surveyId)).thenReturn(Optional.of(new Survey()));
        when(surveySendingPolicyService.getSurveysSendingPolicyById(surveyId))
                .thenReturn(List.of());

        boolean isValid = validator.isValid(sendSurveyResponseDto, context);

        assertFalse(isValid);
        verify(context).buildConstraintViolationWithTemplate("The survey is not active");
    }

    @Test
    void shouldFailWhenSurveyIsNotActive() {
        UUID surveyId = UUID.randomUUID();
        SendSurveyResponseDto sendSurveyResponseDto = new SendSurveyResponseDto();
        sendSurveyResponseDto.setSurveyId(surveyId);
        sendSurveyResponseDto.setAnswers(List.of());

        SurveySendingPolicyTimesDto pastTimeSlot = new SurveySendingPolicyTimesDto();
        pastTimeSlot.setStart(OffsetDateTime.now().minusDays(2));
        pastTimeSlot.setFinish(OffsetDateTime.now().minusDays(1));

        SurveySendingPolicyDto invalidPolicy = new SurveySendingPolicyDto();
        invalidPolicy.setId(UUID.randomUUID());
        invalidPolicy.setSurveyId(surveyId);
        invalidPolicy.setTimeSlots(List.of(pastTimeSlot));

        when(surveyRepository.findById(surveyId)).thenReturn(Optional.of(new Survey()));
        when(surveySendingPolicyService.getSurveysSendingPolicyById(surveyId))
                .thenReturn(List.of(invalidPolicy));

        boolean isValid = validator.isValid(sendSurveyResponseDto, context);

        assertFalse(isValid);
        verify(context).buildConstraintViolationWithTemplate("The survey is not active");
    }

    private SurveySendingPolicyDto validSurveySendingPolicy(UUID surveyId) {
        SurveySendingPolicyTimesDto pastTimeSlot = new SurveySendingPolicyTimesDto();
        pastTimeSlot.setStart(OffsetDateTime.now().minusDays(2));
        pastTimeSlot.setFinish(OffsetDateTime.now().plusDays(1));

        SurveySendingPolicyDto surveySendingPolicyDto = new SurveySendingPolicyDto();
        surveySendingPolicyDto.setId(UUID.randomUUID());
        surveySendingPolicyDto.setSurveyId(surveyId);
        surveySendingPolicyDto.setTimeSlots(List.of(pastTimeSlot));

        return surveySendingPolicyDto;
    }

    @Test
    void shouldNotFailWhenTheRequiredQuestionWasForAnotherRespondentsGroup(){
        UUID surveyID = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        IdentityUser identityUser = new IdentityUser(userId, "John", "pswd", "Respondent");
        RespondentData respondent = new RespondentData();
        respondent.setIdentityUserId(userId);
        Survey survey = new Survey();
        survey.setId(surveyID);
        SurveySection section = new SurveySection();

        List<RespondentGroupDto> respondentGroups = Stream.of(new RespondentGroupDto(UUID.randomUUID(), "group"))
                .toList();

        RespondentGroup targetGroup = new RespondentGroup();
        SectionToUserGroup groupToSection = new SectionToUserGroup();
        groupToSection.setGroup(targetGroup);

        section.setVisibility(Visibility.group_specific);

        Question requiredQuestion = new Question();
        requiredQuestion.setRequired(true);
        requiredQuestion.setQuestionType(QuestionType.yes_no_selection);
        section.setQuestions(Stream.of(requiredQuestion).collect(Collectors.toList()));
        section.setSectionToUserGroups(Stream.of(groupToSection).toList());
        survey.setSections(Stream.of(section).collect(Collectors.toList()));
        when(surveyRepository.findById(surveyID)).thenReturn(Optional.of(survey));
        when(surveySendingPolicyService.getSurveysSendingPolicyById(survey.getId()))
                .thenReturn(List.of(validSurveySendingPolicy(survey.getId())));
        when(claimsPrincipalService.findIdentityUser())
                .thenReturn(identityUser);
        when(respondentDataRepository.findByIdentityUserId(userId)).thenReturn(respondent);
        when(respondentGroupService.getRespondentGroups(respondent.getId()))
                .thenReturn(respondentGroups);
        SendSurveyResponseDto dto = new SendSurveyResponseDto();
        dto.setSurveyId(surveyID);
        dto.setAnswers(new ArrayList<>());
        boolean isValid = validator.isValid(dto, context);
        assertTrue(isValid);
    }

    @Test
    void shouldFailWhenTheRequiredQuestionWasForRespondentsGroupOfTheRespondentAndAnswerIsNotGIven(){
        UUID surveyID = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        IdentityUser identityUser = new IdentityUser(userId, "John", "pswd", "Respondent");
        RespondentData respondent = new RespondentData();
        respondent.setIdentityUserId(userId);
        Survey survey = new Survey();
        survey.setId(surveyID);
        SurveySection section = new SurveySection();

        UUID groupId = UUID.randomUUID();
        List<RespondentGroupDto> respondentGroups = Stream.of(new RespondentGroupDto(groupId, "group"))
                .toList();

        RespondentGroup targetGroup = new RespondentGroup();
        targetGroup.setId(groupId);
        SectionToUserGroup groupToSection = new SectionToUserGroup();
        groupToSection.setGroup(targetGroup);

        section.setVisibility(Visibility.group_specific);

        Question requiredQuestion = new Question();
        requiredQuestion.setRequired(true);
        requiredQuestion.setQuestionType(QuestionType.yes_no_selection);
        section.setQuestions(Stream.of(requiredQuestion).collect(Collectors.toList()));
        section.setSectionToUserGroups(Stream.of(groupToSection).toList());
        survey.setSections(Stream.of(section).collect(Collectors.toList()));
        when(surveyRepository.findById(surveyID)).thenReturn(Optional.of(survey));
        when(surveySendingPolicyService.getSurveysSendingPolicyById(survey.getId()))
                .thenReturn(List.of(validSurveySendingPolicy(survey.getId())));
        when(claimsPrincipalService.findIdentityUser())
                .thenReturn(identityUser);
        when(respondentDataRepository.findByIdentityUserId(userId)).thenReturn(respondent);
        when(respondentGroupService.getRespondentGroups(respondent.getId()))
                .thenReturn(respondentGroups);
        SendSurveyResponseDto dto = new SendSurveyResponseDto();
        dto.setSurveyId(surveyID);
        dto.setAnswers(new ArrayList<>());
        boolean isValid = validator.isValid(dto, context);
        assertFalse(isValid);
    }

    @Test
    void shouldNotFailWhenTheQuestionIsRequiredButWasNotShownBecauseItWasNotTriggered() {
        UUID surveyId = UUID.randomUUID();
        UUID firstQuestionId = UUID.randomUUID();
        UUID firstQuestionSelectedAnswerId = UUID.randomUUID();

        Option optionToBeSelected =   new Option()
                .setId(firstQuestionSelectedAnswerId);

        Survey survey = new Survey()
                .setId(surveyId)
                .setSections(Stream.of(
                        new SurveySection()
                                .setQuestions(Stream.of(
                                        new Question()
                                                .setId(firstQuestionId)
                                                .setOptions(
                                                        Stream.of(
                                                                new Option()
                                                                        .setShowSection(2),
                                                                optionToBeSelected
                                                        ).toList()
                                                )
                                                .setRequired(true)
                                                .setQuestionType(QuestionType.single_text_selection)
                                ).toList())
                                .setVisibility(Visibility.always)
                                .setOrder(1),
                        new SurveySection()
                                .setQuestions(Stream.of(
                                        new Question()
                                                .setRequired(true)
                                                .setQuestionType(QuestionType.yes_no_selection)
                                ).toList())
                                .setVisibility(Visibility.answer_triggered)
                                .setOrder(2)
                ).toList());

        when(surveyRepository.findById(surveyId)).thenReturn(Optional.of(survey));
        when(surveySendingPolicyService.getSurveysSendingPolicyById(survey.getId()))
                .thenReturn(List.of(validSurveySendingPolicy(survey.getId())));

        when(optionRepository.findByIdIn(Stream.of(firstQuestionSelectedAnswerId).toList()))
                .thenReturn(Stream.of(optionToBeSelected).toList());

        SendSurveyResponseDto response = new SendSurveyResponseDto()
                .setSurveyId(surveyId)
                .setAnswers(Stream.of(
                        new AnswerDto()
                                .setQuestionId(firstQuestionId)
                                .setSelectedOptions(Stream.of(
                                        new SelectedOptionDto()
                                                .setOptionId(firstQuestionSelectedAnswerId)
                                ).toList())
                ).toList());

        boolean isValid = validator.isValid(response, context);
        assertTrue(isValid);
    }

    @Test
    void shouldFailWhenTheQuestionIsRequiredAndWasShownBecauseItWasTriggered() {
        UUID surveyId = UUID.randomUUID();
        UUID firstQuestionId = UUID.randomUUID();
        UUID firstQuestionSelectedAnswerId = UUID.randomUUID();

        Option optionToBeSelected =   new Option()
                .setId(firstQuestionSelectedAnswerId)
                .setShowSection(2);

        Survey survey = new Survey()
                .setId(surveyId)
                .setSections(Stream.of(
                        new SurveySection()
                                .setQuestions(Stream.of(
                                        new Question()
                                                .setId(firstQuestionId)
                                                .setOptions(
                                                        Stream.of(
                                                                new Option(),
                                                                optionToBeSelected
                                                        ).toList()
                                                )
                                                .setRequired(true)
                                                .setQuestionType(QuestionType.single_text_selection)
                                ).toList())
                                .setVisibility(Visibility.always)
                                .setOrder(1),
                        new SurveySection()
                                .setQuestions(Stream.of(
                                        new Question()
                                                .setRequired(true)
                                                .setQuestionType(QuestionType.yes_no_selection)
                                ).toList())
                                .setVisibility(Visibility.answer_triggered)
                                .setOrder(2)
                ).toList());

        when(surveyRepository.findById(surveyId)).thenReturn(Optional.of(survey));
        when(surveySendingPolicyService.getSurveysSendingPolicyById(survey.getId()))
                .thenReturn(List.of(validSurveySendingPolicy(survey.getId())));

        when(optionRepository.findByIdIn(Stream.of(firstQuestionSelectedAnswerId).toList()))
                .thenReturn(Stream.of(optionToBeSelected).toList());

        SendSurveyResponseDto response = new SendSurveyResponseDto()
                .setSurveyId(surveyId)
                .setAnswers(Stream.of(
                        new AnswerDto()
                                .setQuestionId(firstQuestionId)
                                .setSelectedOptions(Stream.of(
                                        new SelectedOptionDto()
                                                .setOptionId(firstQuestionSelectedAnswerId)
                                ).toList())
                ).toList());

        boolean isValid = validator.isValid(response, context);
        assertFalse(isValid);
    }

    @Test
    void shouldNotFailWhenTheQuestionIsRequiredAndWasNotShownBecauseItWasTriggeredAndTheAnswerWasGiven(){
        UUID surveyId = UUID.randomUUID();
        UUID firstQuestionId = UUID.randomUUID();
        UUID secondQuestionId = UUID.randomUUID();
        UUID firstQuestionSelectedAnswerId = UUID.randomUUID();

        Option optionToBeSelected =   new Option()
                .setId(firstQuestionSelectedAnswerId)
                .setShowSection(2);

        Survey survey = new Survey()
                .setId(surveyId)
                .setSections(Stream.of(
                        new SurveySection()
                                .setQuestions(Stream.of(
                                        new Question()
                                                .setId(firstQuestionId)
                                                .setOptions(
                                                        Stream.of(
                                                                new Option(),
                                                                optionToBeSelected
                                                        ).toList()
                                                )
                                                .setRequired(true)
                                                .setQuestionType(QuestionType.single_text_selection)
                                ).toList())
                                .setVisibility(Visibility.always)
                                .setOrder(1),
                        new SurveySection()
                                .setQuestions(Stream.of(
                                        new Question()
                                                .setRequired(true)
                                                .setQuestionType(QuestionType.yes_no_selection)
                                                .setId(secondQuestionId)
                                ).toList())
                                .setVisibility(Visibility.answer_triggered)
                                .setOrder(2)
                ).toList());

        when(surveyRepository.findById(surveyId)).thenReturn(Optional.of(survey));
        when(surveySendingPolicyService.getSurveysSendingPolicyById(survey.getId()))
                .thenReturn(List.of(validSurveySendingPolicy(survey.getId())));

        when(optionRepository.findByIdIn(Stream.of(firstQuestionSelectedAnswerId).toList()))
                .thenReturn(Stream.of(optionToBeSelected).toList());

        SendSurveyResponseDto response = new SendSurveyResponseDto()
                .setSurveyId(surveyId)
                .setAnswers(Stream.of(
                        new AnswerDto()
                                .setQuestionId(firstQuestionId)
                                .setSelectedOptions(Stream.of(
                                        new SelectedOptionDto()
                                                .setOptionId(firstQuestionSelectedAnswerId)
                                ).toList()),
                        new AnswerDto()
                                .setQuestionId(secondQuestionId)
                                .setYesNoAnswer(true)
                ).toList());

        boolean isValid = validator.isValid(response, context);
        assertTrue(isValid);
    }

    @Test
    void shouldPassWhenTheQuestionTypeIsMultipleChoiceAndTheAnswersAreGiven(){
        UUID surveyId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        UUID firstOptionId = UUID.randomUUID();
        UUID secondOptionId = UUID.randomUUID();
        UUID thirdOptionId = UUID.randomUUID();
        List<Option> options = Stream.of(
                new Option().setId(firstOptionId),
                new Option().setId(secondOptionId),
                new Option().setId(thirdOptionId)
        ).toList();

        Survey survey = new Survey()
                .setId(UUID.randomUUID())
                .setSections(Stream.of(
                        new SurveySection()
                                .setVisibility(Visibility.always)
                                .setQuestions(Stream.of(
                                        new Question()
                                                .setQuestionType(QuestionType.multiple_choice)
                                                .setId(questionId)
                                                .setOptions(options)
                                                .setRequired(true)
                                ).toList())
                ).toList());

        when(surveyRepository.findById(surveyId)).thenReturn(Optional.of(survey));
        when(surveySendingPolicyService.getSurveysSendingPolicyById(survey.getId()))
                .thenReturn(List.of(validSurveySendingPolicy(survey.getId())));
        when(optionRepository.findByIdIn(options.stream().map(Option::getId).toList()))
                .thenReturn(options);

        SendSurveyResponseDto response = new SendSurveyResponseDto()
                .setSurveyId(surveyId)
                .setAnswers(Stream.of(
                        new AnswerDto()
                                .setQuestionId(questionId)
                                .setSelectedOptions(Stream.of(
                                        new SelectedOptionDto()
                                                .setOptionId(firstOptionId),
                                        new SelectedOptionDto().setOptionId(secondOptionId)
                                ).toList())
                ).toList());

        boolean isValid = validator.isValid(response, context);
        assertTrue(isValid);
    }

    @Test
    void shouldFailWhenTheQuestionTypeIsMultipleChoiceAndOneOfTheAnswersIsBad(){
        UUID surveyId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        UUID firstOptionId = UUID.randomUUID();
        UUID secondOptionId = UUID.randomUUID();
        UUID thirdOptionId = UUID.randomUUID();
        List<Option> options = Stream.of(
                new Option().setId(firstOptionId),
                new Option().setId(secondOptionId),
                new Option().setId(thirdOptionId)
        ).toList();

        Survey survey = new Survey()
                .setId(UUID.randomUUID())
                .setSections(Stream.of(
                        new SurveySection()
                                .setVisibility(Visibility.always)
                                .setQuestions(Stream.of(
                                        new Question()
                                                .setQuestionType(QuestionType.multiple_choice)
                                                .setId(questionId)
                                                .setOptions(options)
                                                .setRequired(true)
                                ).toList())
                ).toList());

        when(surveyRepository.findById(surveyId)).thenReturn(Optional.of(survey));
        when(surveySendingPolicyService.getSurveysSendingPolicyById(survey.getId()))
                .thenReturn(List.of(validSurveySendingPolicy(survey.getId())));
        when(optionRepository.findByIdIn(options.stream().map(Option::getId).toList()))
                .thenReturn(options);

        SendSurveyResponseDto response = new SendSurveyResponseDto()
                .setSurveyId(surveyId)
                .setAnswers(Stream.of(
                        new AnswerDto()
                                .setQuestionId(questionId)
                                .setSelectedOptions(Stream.of(
                                        new SelectedOptionDto()
                                                .setOptionId(firstOptionId),
                                        new SelectedOptionDto().setOptionId(secondOptionId),
                                        new SelectedOptionDto().setOptionId(UUID.randomUUID())
                                ).toList())
                ).toList());

        boolean isValid = validator.isValid(response, context);
        assertFalse(isValid);
    }

    @Test
    void shouldFailWhenNoMultipleChoiceAnswerIsGivenAndQuestionIsRequired(){
        UUID surveyId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        UUID firstOptionId = UUID.randomUUID();
        UUID secondOptionId = UUID.randomUUID();
        UUID thirdOptionId = UUID.randomUUID();
        List<Option> options = Stream.of(
                new Option().setId(firstOptionId),
                new Option().setId(secondOptionId),
                new Option().setId(thirdOptionId)
        ).toList();

        Survey survey = new Survey()
                .setId(UUID.randomUUID())
                .setSections(Stream.of(
                        new SurveySection()
                                .setVisibility(Visibility.always)
                                .setQuestions(Stream.of(
                                        new Question()
                                                .setQuestionType(QuestionType.multiple_choice)
                                                .setId(questionId)
                                                .setOptions(options)
                                                .setRequired(true)
                                ).toList())
                ).toList());

        when(surveyRepository.findById(surveyId)).thenReturn(Optional.of(survey));
        when(surveySendingPolicyService.getSurveysSendingPolicyById(survey.getId()))
                .thenReturn(List.of(validSurveySendingPolicy(survey.getId())));
        when(optionRepository.findByIdIn(options.stream().map(Option::getId).toList()))
                .thenReturn(options);

        SendSurveyResponseDto response = new SendSurveyResponseDto()
                .setSurveyId(surveyId)
                .setAnswers(Stream.of(
                        new AnswerDto()
                                .setQuestionId(questionId)
                                .setSelectedOptions(new ArrayList<>())
                ).toList());

        boolean isValid = validator.isValid(response, context);
        assertFalse(isValid);
    }

    @Test
    void shouldReturnFalseWhenSendSurveyResponseDtoIsNull() {
        SendSurveyResponseDto sendSurveyResponseDto = null;

        boolean result = validator.isValid(sendSurveyResponseDto, context);

        assertFalse(result);
        verify(context).buildConstraintViolationWithTemplate("Survey response data is invalid");
        verify(violationBuilder).addPropertyNode("surveyId");
    }

    @Test
    void shouldReturnFalseWhenSurveyIdIsNull() {
        SendSurveyResponseDto sendSurveyResponseDto = new SendSurveyResponseDto();
        sendSurveyResponseDto.setSurveyId(null);
        sendSurveyResponseDto.setAnswers(Collections.emptyList());

        boolean result = validator.isValid(sendSurveyResponseDto, context);

        assertFalse(result);
        verify(context).buildConstraintViolationWithTemplate("Survey response data is invalid");
        verify(violationBuilder).addPropertyNode("surveyId");
    }

    @Test
    void shouldReturnFalseWhenAnswersAreNull() {
        SendSurveyResponseDto sendSurveyResponseDto = new SendSurveyResponseDto();
        sendSurveyResponseDto.setSurveyId(UUID.randomUUID());
        sendSurveyResponseDto.setAnswers(null);

        boolean result = validator.isValid(sendSurveyResponseDto, context);

        assertFalse(result);
        verify(context).buildConstraintViolationWithTemplate("Survey response data is invalid");
        verify(violationBuilder).addPropertyNode("surveyId");
    }

}
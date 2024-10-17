package com.survey.api.validation;

import com.survey.application.dtos.RespondentGroupDto;
import com.survey.application.dtos.SurveySendingPolicyDto;
import com.survey.application.dtos.surveyDtos.AnswerDto;
import com.survey.application.dtos.surveyDtos.SelectedOptionDto;
import com.survey.application.dtos.surveyDtos.SendSurveyResponseDto;
import com.survey.application.services.ClaimsPrincipalService;
import com.survey.application.services.RespondentGroupService;
import com.survey.application.services.SurveySendingPolicyService;
import com.survey.domain.models.Option;
import com.survey.domain.models.Question;
import com.survey.domain.models.Survey;
import com.survey.domain.models.SurveySection;
import com.survey.domain.repository.OptionRepository;
import com.survey.domain.repository.RespondentDataRepository;
import com.survey.domain.repository.SurveyRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;


public class SendSurveyResponseDtoValidator
implements ConstraintValidator<ValidSendSurveyResponse, SendSurveyResponseDto> {

    private final SurveyRepository surveyRepository;
    private final SurveySendingPolicyService surveySendingPolicyService;
    private final RespondentGroupService respondentGroupService;
    private final ClaimsPrincipalService claimsPrincipalService;
    private final RespondentDataRepository respondentDataRepository;
    private final OptionRepository optionRepository;




    public SendSurveyResponseDtoValidator(SurveyRepository surveyRepository,
                                          SurveySendingPolicyService surveySendingPolicyService,
                                          RespondentGroupService respondentGroupService,
                                          ClaimsPrincipalService claimsPrincipalService,
                                          RespondentDataRepository respondentDataRepository,
                                          OptionRepository optionRepository){

        this.surveyRepository = surveyRepository;
        this.surveySendingPolicyService = surveySendingPolicyService;
        this.respondentGroupService = respondentGroupService;
        this.claimsPrincipalService = claimsPrincipalService;
        this.respondentDataRepository = respondentDataRepository;
        this.optionRepository = optionRepository;
    }


    @Override
    @Transactional
    public boolean isValid(SendSurveyResponseDto sendSurveyResponseDto, ConstraintValidatorContext constraintValidatorContext) {
        if (
                sendSurveyResponseDto == null ||
                sendSurveyResponseDto.getSurveyId() == null ||
                sendSurveyResponseDto.getAnswers() == null)
        {
            constraintValidatorContext
                    .buildConstraintViolationWithTemplate("Survey response data is invalid")
                    .addPropertyNode("surveyId")
                    .addConstraintViolation();
            return false;
        }

        Optional<Survey> surveyOptional = surveyRepository.findById(sendSurveyResponseDto.getSurveyId());

        if (surveyOptional.isEmpty()){
            constraintValidatorContext
                    .buildConstraintViolationWithTemplate("This survey does not exist")
                    .addPropertyNode("surveyId")
                    .addConstraintViolation();
            return false;
        }

        Survey survey = surveyOptional.get();
        boolean isValid = true;

        if (!validateIfTheSurveyIsActive(survey.getId(), constraintValidatorContext)) {
            return false;
        }

        if (!validateAllRequiredQuestionsAnswered(survey, sendSurveyResponseDto.getAnswers(), constraintValidatorContext)) {
            return false;
        }

        Map<UUID, Question> questionIdMappings =
                survey
                        .getSections()
                        .stream()
                        .flatMap(x -> x.getQuestions().stream())
                        .collect(Collectors.toMap(Question::getId, x -> x));

        Map<UUID, Boolean> answerFoundMappings =
                survey
                        .getSections()
                        .stream()
                        .flatMap(x -> x.getQuestions().stream())
                        .collect(Collectors.toMap(Question::getId, x -> false));

        for (AnswerDto answerDto : sendSurveyResponseDto.getAnswers()){
            answerFoundMappings.put(answerDto.getQuestionId(), true);
            if (!questionIdMappings.containsKey(answerDto.getQuestionId())){
                constraintValidatorContext
                        .buildConstraintViolationWithTemplate("Each answer must match an existing question for specified survey")
                        .addPropertyNode("answers")
                        .addConstraintViolation();
                isValid = false;
                continue;
            }

            Question matchingQuestion = questionIdMappings.get(answerDto.getQuestionId());
            if (!validateAnswerWithQuestionType(answerDto, matchingQuestion, constraintValidatorContext)){
                isValid = false;
            }
        }
        return isValid;
    }

    private boolean validateAnswerWithQuestionType(AnswerDto answer,
                                                Question question, ConstraintValidatorContext ctx){
        switch (question.getQuestionType()){
            case yes_no_selection:
                return validateYesNo(answer, ctx);
            case single_text_selection:
                return validateChoice(question, answer, ctx, false);
            case multiple_choice:
                return validateChoice(question, answer, ctx, true);
            case discrete_number_selection:
                return validateNumberRange(question, answer, ctx);
            case number_selection:
                return validateNumericAnswer(question, answer, ctx, "Numeric answer");
            default:
                throw new IllegalArgumentException();
        }
    }


    private boolean validateNumericAnswer(Question question, AnswerDto answerDto, ConstraintValidatorContext ctx, String answerTypeName) {
        boolean result = true;
        if (answerDto.getNumericAnswer() == null){
            ctx
                    .buildConstraintViolationWithTemplate(answerTypeName + " must have a numeric value")
                    .addPropertyNode("answers")
                    .addConstraintViolation();
            result = false;
        }
        if (answerDto.getSelectedOptions() != null && !answerDto.getSelectedOptions().isEmpty()){
            ctx
                    .buildConstraintViolationWithTemplate(answerTypeName + " must not have a selected options")
                    .addPropertyNode("answers")
                    .addConstraintViolation();
            result = false;
        }

        if (answerDto.getYesNoAnswer() != null){
            ctx
                    .buildConstraintViolationWithTemplate(answerTypeName + " answer must not have a yes/no answer specified")
                    .addPropertyNode("answers")
                    .addConstraintViolation();
            result = false;
        }
        return result;
    }

    private boolean validateYesNo(AnswerDto answerDto, ConstraintValidatorContext ctx){
        boolean result = true;
        if (answerDto.getNumericAnswer() != null){
            ctx
                    .buildConstraintViolationWithTemplate("'Yes/No' answer must not have a numeric value")
                    .addPropertyNode("answers")
                    .addConstraintViolation();
            result = false;
        }

        if (answerDto.getSelectedOptions() != null && !answerDto.getSelectedOptions().isEmpty()){
            ctx
                    .buildConstraintViolationWithTemplate("'Yes/No' answer must not have a selected options")
                    .addPropertyNode("answers")
                    .addConstraintViolation();
            result = false;
        }

        if (answerDto.getYesNoAnswer() == null){
            ctx
                    .buildConstraintViolationWithTemplate("'Yes/No' answer must have a yes/no answer specified")
                    .addPropertyNode("answers")
                    .addConstraintViolation();
            result = false;
        }

        return result;
    }

    private boolean validateNumberRange(Question question, AnswerDto answerDto, ConstraintValidatorContext ctx){
        boolean result = validateNumericAnswer(question, answerDto, ctx, "Linear scale answer");

        if (result) {
            if (answerDto.getNumericAnswer() > question.getNumberRange().getTo() ||
                    answerDto.getNumericAnswer() < question.getNumberRange().getFrom()) {
                ctx.buildConstraintViolationWithTemplate("Answer violates a number range constraint")
                        .addPropertyNode("answers")
                        .addConstraintViolation();
                result = false;
            }
        }
        return result;
    }

    private boolean validateChoice(Question question, AnswerDto answerDto,
                                   ConstraintValidatorContext ctx, boolean multiple){
        String errorMessagePrefix = multiple ? "Multiple" : "Single";
        boolean result = true;
        if (answerDto.getNumericAnswer() != null){
            ctx
                    .buildConstraintViolationWithTemplate(errorMessagePrefix + " choice answer must not have a numeric value")
                    .addPropertyNode("answers")
                    .addConstraintViolation();
            result = false;
        }

        if (answerDto.getYesNoAnswer() != null){
            ctx
                    .buildConstraintViolationWithTemplate(errorMessagePrefix + " choice answer must not have a yes/no answer specified")
                    .addPropertyNode("answers")
                    .addConstraintViolation();
            result = false;
        }

        List<SelectedOptionDto> selectedOptions = answerDto.getSelectedOptions();
        if (!multiple && (selectedOptions == null || selectedOptions.size() != 1)){
            ctx
                    .buildConstraintViolationWithTemplate(errorMessagePrefix + " choice answer must not have exactly one selected option")
                    .addPropertyNode("answers")
                    .addConstraintViolation();
            result = false;
        }

        if (multiple && (selectedOptions == null || selectedOptions.isEmpty())){
            ctx
                    .buildConstraintViolationWithTemplate(errorMessagePrefix + " choice answer must not have at least one selected option")
                    .addPropertyNode("answers")
                    .addConstraintViolation();
            result = false;
        }

        HashSet<UUID> optionsIds = question
                .getOptions()
                .stream()
                .map(Option::getId)
                .collect(Collectors.toCollection(HashSet::new));

        if (selectedOptions.stream().anyMatch(x -> !optionsIds.contains(x.getOptionId()))){
            ctx
                    .buildConstraintViolationWithTemplate(errorMessagePrefix + " choice answer must have a selected option matching available option for the proper question")
                    .addPropertyNode("answers")
                    .addConstraintViolation();

            result = false;
        }
        return result;
    }

    private boolean validateIfTheSurveyIsActive(UUID surveyId, ConstraintValidatorContext ctx){
        OffsetDateTime now = OffsetDateTime.now();

        List<SurveySendingPolicyDto> activeTimeSlots = surveySendingPolicyService.getSurveysSendingPolicyById(surveyId);

        boolean isActive = activeTimeSlots.stream().anyMatch(policy ->
                policy.getTimeSlots().stream().anyMatch(slot ->
                        now.isAfter(slot.getStart()) && now.isBefore(slot.getFinish())
                )
        );

        if (!isActive) {
            ctx.buildConstraintViolationWithTemplate("The survey is not active")
                    .addPropertyNode("surveyId")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }

    private boolean isVisibleToParticipant(SurveySection section, Question question, List<AnswerDto> answers) {
        return switch (section.getVisibility()) {
            case always -> true;
            case group_specific -> checkGroupSpecificVisibility(section);
            case answer_triggered -> checkAnswerTriggeredVisibility(section, answers);
        };
    }

    private boolean validateAllRequiredQuestionsAnswered(Survey survey, List<AnswerDto> answers, ConstraintValidatorContext ctx) {
        Set<UUID> answeredQuestionIds = answers.stream()
                .map(AnswerDto::getQuestionId)
                .collect(Collectors.toSet());

        boolean allRequiredQuestionsAnswered = survey.getSections().stream()
                .flatMap(section -> section.getQuestions().stream()
                        .filter(question -> question.getRequired() && isVisibleToParticipant(section, question, answers))
                        .filter(question -> !answeredQuestionIds.contains(question.getId()))
                ).findAny().isEmpty();

        if (!allRequiredQuestionsAnswered) {
            ctx.buildConstraintViolationWithTemplate("All required questions must be answered")
                    .addPropertyNode("answers")
                    .addConstraintViolation();
        }

        return allRequiredQuestionsAnswered;
    }

    private boolean checkGroupSpecificVisibility(SurveySection section) {
        UUID identityUserId = claimsPrincipalService.findIdentityUser().getId();
        UUID respondentId = respondentDataRepository.findByIdentityUserId(identityUserId).getId();
        List<RespondentGroupDto> respondentGroups = respondentGroupService.getRespondentGroups(respondentId);

        List<UUID> respondentGroupIds = respondentGroups.stream()
                .map(RespondentGroupDto::getId)
                .toList();

        return section.getSectionToUserGroups().stream()
                .anyMatch(group -> respondentGroupIds.contains(group.getGroup().getId()));
    }

    private boolean checkAnswerTriggeredVisibility(SurveySection section, List<AnswerDto> answers) {
        List<UUID> selectedOptionIds = new ArrayList<>();
        for (AnswerDto answer : answers) {
            if (answer.getSelectedOptions() != null) {
                for (SelectedOptionDto selectedOption : answer.getSelectedOptions()) {
                    selectedOptionIds.add(selectedOption.getOptionId());
                }
            }
        }
        List<Option> options = optionRepository.findByIdIn(selectedOptionIds);
        for (Option option : options) {
            if (option.getShowSection() != null && option.getShowSection().equals(section.getOrder())) {
                return true;
            }
        }
        return false;
    }
}


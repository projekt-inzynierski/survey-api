package com.survey.application.services;

import com.survey.api.validation.SendSurveyResponseDtoValidator;
import com.survey.application.dtos.LocalizationPointDto;
import com.survey.application.dtos.SurveyResultDto;
import com.survey.application.dtos.surveyDtos.*;
import com.survey.domain.models.*;
import com.survey.domain.models.enums.QuestionType;
import com.survey.domain.repository.OptionRepository;
import com.survey.domain.repository.QuestionRepository;
import com.survey.domain.repository.SurveyParticipationRepository;
import com.survey.domain.repository.SurveyRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.*;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.annotation.RequestScope;

import javax.management.InvalidAttributeValueException;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequestScope
public class SurveyResponsesServiceImpl implements SurveyResponsesService {
    private final SurveyParticipationRepository surveyParticipationRepository;
    private final SurveyRepository surveyRepository;
    private final OptionRepository optionRepository;
    private final QuestionRepository questionRepository;
    private final ClaimsPrincipalServiceImpl claimsPrincipalServiceImpl;
    private final ModelMapper modelMapper;
    private final EntityManager entityManager;
    private final SendSurveyResponseDtoValidator sendSurveyResponseDtoValidator;
    private final SurveyParticipationTimeValidationService surveyParticipationTimeValidationService;



    @Autowired
    public SurveyResponsesServiceImpl(
            SurveyParticipationRepository surveyParticipationRepository,
            SurveyRepository surveyRepository,
            OptionRepository optionRepository,
            QuestionRepository questionRepository,
            ClaimsPrincipalServiceImpl claimsPrincipalServiceImpl,
            ModelMapper modelMapper,
            EntityManager entityManager, SendSurveyResponseDtoValidator sendSurveyResponseDtoValidator, SurveyParticipationTimeValidationService surveyParticipationTimeValidationService) {
        this.surveyParticipationRepository = surveyParticipationRepository;
        this.surveyRepository = surveyRepository;
        this.optionRepository = optionRepository;
        this.questionRepository = questionRepository;
        this.claimsPrincipalServiceImpl = claimsPrincipalServiceImpl;
        this.modelMapper = modelMapper;
        this.entityManager = entityManager;
        this.sendSurveyResponseDtoValidator = sendSurveyResponseDtoValidator;
        this.surveyParticipationTimeValidationService = surveyParticipationTimeValidationService;
    }

    Survey findSurveyById(UUID surveyId) {
        return surveyRepository.findById(surveyId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid survey ID - survey doesn't exist"));
    }

    private List<Question> findQuestionsByIds(List<UUID> questionIds, UUID surveyId) {
        return questionRepository.findAllByIds(surveyId, questionIds);
    }


    private SurveyParticipation saveSurveyParticipationOnline(IdentityUser identityUser, Survey survey, OffsetDateTime surveyStartDate, OffsetDateTime surveyFinishDate){
        OffsetDateTime surveyParticipationDateToSave = surveyParticipationTimeValidationService
                .getCorrectSurveyParticipationDateTimeOnline(identityUser.getId(), survey.getId(), surveyStartDate, surveyFinishDate);

        SurveyParticipation participation = new SurveyParticipation();
        participation.setIdentityUser(identityUser);
        participation.setDate(surveyParticipationDateToSave);
        participation.setSurvey(survey);
        return participation;
    }

    private SurveyParticipation saveSurveyParticipationOffline(IdentityUser identityUser, Survey survey, OffsetDateTime surveyStartDate, OffsetDateTime surveyFinishDate){
        OffsetDateTime surveyParticipationDateToSave = surveyParticipationTimeValidationService
                .getCorrectSurveyParticipationDateTimeOffline(identityUser.getId(), survey.getId(), surveyStartDate, surveyFinishDate);

        if (surveyParticipationDateToSave == null){
            return null;
        }

        SurveyParticipation participation = new SurveyParticipation();
        participation.setIdentityUser(identityUser);
        participation.setDate(surveyParticipationDateToSave);
        participation.setSurvey(survey);
        return participation;
    }

    private Map<UUID, Option> findOptionsBySurveyId(List<UUID> questionIds) {
        return optionRepository.findByQuestionIdIn(questionIds)
                .stream()
                .collect(Collectors.toMap(Option::getId, option -> option));
    }

    private SurveyParticipation mapQuestionAnswers(SendSurveyResponseDto sendSurveyResponseDto, SurveyParticipation surveyParticipation, Survey survey) {
        List<UUID> questionIds = sendSurveyResponseDto.getAnswers().stream()
                .map(AnswerDto::getQuestionId)
                .collect(Collectors.toList());

        List<Question> questions = findQuestionsByIds(questionIds, survey.getId());
        Map<UUID, Question> questionMap = questions.stream()
                .collect(Collectors.toMap(Question::getId, question -> question));

        Map<UUID, Option> optionsMap = findOptionsBySurveyId(questionIds);

        List<QuestionAnswer> questionAnswers = sendSurveyResponseDto.getAnswers().stream()
                .map(answerDto -> {
                    Question question = questionMap.get(answerDto.getQuestionId());
                    if (question == null) {
                        throw new IllegalArgumentException("Invalid question ID: " + answerDto.getQuestionId());
                    }
                    QuestionAnswer questionAnswer = new QuestionAnswer();
                    questionAnswer.setSurveyParticipation(surveyParticipation);
                    questionAnswer.setQuestion(question);

                    if (question.getQuestionType().equals(QuestionType.single_choice) || question.getQuestionType().equals(QuestionType.multiple_choice) || question.getQuestionType().equals(QuestionType.image_choice)) {
                        List<OptionSelection> optionSelections = answerDto.getSelectedOptions().stream()
                                .map(selectedOptionDto -> {
                                    Option option = optionsMap.get(selectedOptionDto.getOptionId());
                                    OptionSelection optionSelection = new OptionSelection();
                                    optionSelection.setQuestionAnswer(questionAnswer);
                                    optionSelection.setOption(option);
                                    return optionSelection;
                                }).collect(Collectors.toList());
                        questionAnswer.setOptionSelections(optionSelections);
                    }

                    if (question.getQuestionType().equals(QuestionType.yes_no_choice)) {
                        questionAnswer.setYesNoAnswer(answerDto.getYesNoAnswer());
                    }

                    if (question.getQuestionType().equals(QuestionType.number_input) || question.getQuestionType().equals(QuestionType.linear_scale)) {
                        questionAnswer.setNumericAnswer(answerDto.getNumericAnswer());
                    }

                    return questionAnswer;
                }).collect(Collectors.toList());

        surveyParticipation.setQuestionAnswers(questionAnswers);
        return surveyParticipation;
    }

    private SurveyParticipationDto mapToDto(SurveyParticipation surveyParticipation, SendSurveyResponseDto sendSurveyResponseDto, IdentityUser identityUser) {
        SurveyParticipation finalSurveyParticipation = surveyParticipationRepository.saveAndFlush(surveyParticipation);
        entityManager.refresh(finalSurveyParticipation);
        SurveyParticipationDto surveyParticipationDto = modelMapper.map(finalSurveyParticipation, SurveyParticipationDto.class);
        surveyParticipationDto.setSurveyId(sendSurveyResponseDto.getSurveyId());
        surveyParticipationDto.setRespondentId(identityUser.getId());
        return surveyParticipationDto;
    }

    @Override
    @Transactional
    public SurveyParticipationDto saveSurveyResponseOnline(SendOnlineSurveyResponseDto sendOnlineSurveyResponseDto) throws InvalidAttributeValueException {
        IdentityUser identityUser = claimsPrincipalServiceImpl.findIdentityUser();
        Survey survey = findSurveyById(sendOnlineSurveyResponseDto.getSurveyId());

        SurveyParticipation surveyParticipation = saveSurveyParticipationOnline(identityUser, survey, sendOnlineSurveyResponseDto.getStartDate(), sendOnlineSurveyResponseDto.getFinishDate());
        SurveyParticipation finalSurveyParticipation = mapQuestionAnswers(sendOnlineSurveyResponseDto, surveyParticipation, survey);
        surveyParticipationRepository.save(finalSurveyParticipation);
        return mapToDto(finalSurveyParticipation, sendOnlineSurveyResponseDto, identityUser);
    }

    @Override
    @Transactional
    public List<SurveyParticipationDto> saveSurveyResponsesOffline(List<SendOfflineSurveyResponseDto> sendOfflineSurveyResponseDtoList) {
        IdentityUser identityUser = claimsPrincipalServiceImpl.findIdentityUser();

        return sendOfflineSurveyResponseDtoList.stream()
                .filter(dto -> sendSurveyResponseDtoValidator.isValid(dto, null))
                .map(dto -> {
                    Survey survey = findSurveyById(dto.getSurveyId());
                    SurveyParticipation participation = saveSurveyParticipationOffline(identityUser, survey, dto.getStartDate(), dto.getFinishDate());
                    if (participation == null) return null;

                    SurveyParticipation finalParticipation = mapQuestionAnswers(dto, participation, survey);
                    surveyParticipationRepository.save(finalParticipation);
                    return mapToDto(finalParticipation, dto, identityUser);
                })
                .filter(Objects::nonNull)
                .toList();
    }


    @Override
    @Transactional
    public List<SurveyResultDto> getSurveyResults(UUID surveyId, UUID identityUserId, OffsetDateTime dateFrom, OffsetDateTime dateTo) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<SurveyParticipation> cq = cb.createQuery(SurveyParticipation.class);

        Root<SurveyParticipation> root = cq.from(SurveyParticipation.class);
        root.fetch("localizationDataList", JoinType.LEFT);

        List<Predicate> predicates = new ArrayList<>();

        if (surveyId != null) {
            predicates.add(cb.equal(root.get("survey").get("id"), surveyId));
        }
        if (identityUserId != null) {
            predicates.add(cb.equal(root.get("identityUser").get("id"), identityUserId));
        }
        if (dateFrom != null && dateTo != null) {
            predicates.add(cb.between(root.get("date"), dateFrom, dateTo));
        } else if (dateFrom != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("date"), dateFrom));
        } else if (dateTo != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("date"), dateTo));
        }

        cq.select(root).where(cb.and(predicates.toArray(new Predicate[0])));
        List<SurveyParticipation> participationList = entityManager.createQuery(cq)
                .getResultList();

        return participationList.stream()
                .flatMap(this::mapParticipationToDto)
                .collect(Collectors.toList());
    }

    private Stream<SurveyResultDto> mapParticipationToDto(SurveyParticipation surveyParticipation) {
        return surveyParticipation.getQuestionAnswers().stream()
                .map(questionAnswer -> createSurveyResultDto(surveyParticipation, questionAnswer));
    }

    private SurveyResultDto createSurveyResultDto(SurveyParticipation surveyParticipation, QuestionAnswer questionAnswer) {
        SurveyResultDto dto = new SurveyResultDto();
        dto.setSurveyName(surveyParticipation.getSurvey().getName());
        dto.setQuestion(questionAnswer.getQuestion().getContent());
        dto.setResponseDate(surveyParticipation.getDate());
        dto.setRespondentId(surveyParticipation.getIdentityUser().getId());
        dto.setAnswers(extractAnswers(questionAnswer));
        dto.setLocalizations(extractLocalizationPoints(surveyParticipation));

        return dto;
    }

    private List<Object> extractAnswers(QuestionAnswer questionAnswer) {
        List<Object> answers = new ArrayList<>();

        Optional.ofNullable(questionAnswer.getNumericAnswer())
                .ifPresent(answers::add);

        if (questionAnswer.getOptionSelections() != null) {
            answers.addAll(questionAnswer.getOptionSelections().stream()
                    .map(optionSelection -> optionSelection.getOption().getLabel())
                    .toList());
        }

        Optional.ofNullable(questionAnswer.getYesNoAnswer())
                .ifPresent(answers::add);

        return answers;
    }

    List<LocalizationPointDto> extractLocalizationPoints(SurveyParticipation surveyParticipation){
        return surveyParticipation.getLocalizationDataList().stream()
                .map(ld -> new LocalizationPointDto(ld.getLatitude(), ld.getLongitude(), ld.getDateTime()))
                .toList();
    }

}

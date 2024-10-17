package com.survey.application.services;

import com.survey.application.dtos.SurveyResultDto;
import com.survey.application.dtos.surveyDtos.AnswerDto;
import com.survey.application.dtos.surveyDtos.SendSurveyResponseDto;
import com.survey.application.dtos.surveyDtos.SurveyParticipationDto;
import com.survey.domain.models.*;
import com.survey.domain.models.enums.QuestionType;
import com.survey.domain.repository.OptionRepository;
import com.survey.domain.repository.QuestionRepository;
import com.survey.domain.repository.SurveyParticipationRepository;
import com.survey.domain.repository.SurveyRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.annotation.RequestScope;

import javax.management.InvalidAttributeValueException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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


    @Autowired
    public SurveyResponsesServiceImpl(
            SurveyParticipationRepository surveyParticipationRepository,
            SurveyRepository surveyRepository,
            OptionRepository optionRepository,
            QuestionRepository questionRepository,
            ClaimsPrincipalServiceImpl claimsPrincipalServiceImpl,
            ModelMapper modelMapper,
            EntityManager entityManager) {
        this.surveyParticipationRepository = surveyParticipationRepository;
        this.surveyRepository = surveyRepository;
        this.optionRepository = optionRepository;
        this.questionRepository = questionRepository;
        this.claimsPrincipalServiceImpl = claimsPrincipalServiceImpl;
        this.modelMapper = modelMapper;
        this.entityManager = entityManager;
    }

    private Survey findSurveyById(UUID surveyId) {
        return surveyRepository.findById(surveyId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid survey ID - survey doesn't exist"));
    }

    private List<Question> findQuestionsByIds(List<UUID> questionIds, UUID surveyId) {
        return questionRepository.findAllByIds(surveyId, questionIds);
    }


    private SurveyParticipation saveSurveyParticipation(SendSurveyResponseDto sendSurveyResponseDto, IdentityUser identityUser, Survey survey) {
        SurveyParticipation surveyParticipation = new SurveyParticipation();
        surveyParticipation.setIdentityUser(identityUser);
        surveyParticipation.setDate(OffsetDateTime.now(ZoneOffset.UTC));
        surveyParticipation.setSurvey(survey);
        return surveyParticipation;
    }
    private Map<UUID, Option> findOptionsBySurveyId(List<UUID> questionIds) {
        return optionRepository.findByQuestionIdIn(questionIds)
                .stream()
                .collect(Collectors.toMap(Option::getId, option -> option));
    }

private SurveyParticipation mapQuestionAnswers(SendSurveyResponseDto sendSurveyResponseDto, SurveyParticipation surveyParticipation, Survey survey) throws InvalidAttributeValueException {
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

                if (question.getQuestionType().equals(QuestionType.discrete_number_selection)) {
                    Integer numericAnswer = answerDto.getNumericAnswer();
                    //TODO: this is more complicated, let's allow null for now
                    //if (numericAnswer == null || numericAnswer < question.getNumberRange().getFrom() || numericAnswer > question.getNumberRange().getTo() ) {

                        //throw new IllegalArgumentException("Invalid Numeric answer.");
                    //}
                    questionAnswer.setNumericAnswer(numericAnswer);
                }
                if (question.getQuestionType().equals(QuestionType.single_text_selection)) {
                    List<OptionSelection> optionSelections = answerDto.getSelectedOptions().stream()
                            .map(selectedOptionDto -> {
                                Option option = optionsMap.get(selectedOptionDto.getOptionId());
                                //TODO: this is more complicated, let's allow null for now
                                //if (option == null) {
                                    //throw new IllegalArgumentException("Invalid option ID: " + selectedOptionDto.getOptionId());
                                //}
                                OptionSelection optionSelection = new OptionSelection();
                                optionSelection.setQuestionAnswer(questionAnswer);
                                optionSelection.setOption(option);
                                return optionSelection;
                            }).collect(Collectors.toList());
                    questionAnswer.setOptionSelections(optionSelections);
                }

                if (question.getQuestionType().equals(QuestionType.yes_no_selection)) {
                    questionAnswer.setYesNoAnswer(answerDto.getYesNoAnswer());
                }

                if (question.getQuestionType().equals(QuestionType.number_selection)) {
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
    public SurveyParticipationDto saveSurveyResponse(SendSurveyResponseDto sendSurveyResponseDto, String token) throws InvalidAttributeValueException {
        IdentityUser identityUser = claimsPrincipalServiceImpl.findIdentityUser();
        Survey survey = findSurveyById(sendSurveyResponseDto.getSurveyId());
        SurveyParticipation surveyParticipation = saveSurveyParticipation(sendSurveyResponseDto, identityUser, survey);
        SurveyParticipation finalSurveyParticipation = mapQuestionAnswers(sendSurveyResponseDto, surveyParticipation, survey);
        surveyParticipationRepository.save(finalSurveyParticipation);
        return mapToDto(finalSurveyParticipation, sendSurveyResponseDto, identityUser);
    }

    @Override
    @Transactional
    public List<SurveyResultDto> getSurveyResults(UUID surveyId, OffsetDateTime dateFrom, OffsetDateTime dateTo) {
        String jpql = "SELECT sp FROM SurveyParticipation sp " +
                "JOIN sp.survey s " +
                "JOIN sp.questionAnswers qa " +
                "WHERE sp.survey.id = :surveyId " +
                "AND sp.date BETWEEN :dateFrom AND :dateTo";

        TypedQuery<SurveyParticipation> query = entityManager.createQuery(jpql, SurveyParticipation.class);
        query.setParameter("surveyId", surveyId);
        query.setParameter("dateFrom", dateFrom);
        query.setParameter("dateTo", dateTo);

        List<SurveyParticipation> participationList = query.getResultList();

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
}

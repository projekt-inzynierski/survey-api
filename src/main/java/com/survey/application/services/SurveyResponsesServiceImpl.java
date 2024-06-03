package com.survey.application.services;

import com.survey.application.dtos.surveyDtos.AnswerDto;
import com.survey.application.dtos.surveyDtos.SelectedOptionDto;
import com.survey.application.dtos.surveyDtos.SendSurveyResponseDto;
import com.survey.application.dtos.surveyDtos.SurveyParticipationDto;
import com.survey.domain.models.*;
import com.survey.domain.models.enums.QuestionType;
import com.survey.domain.repository.*;
import jakarta.persistence.EntityManager;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.management.InvalidAttributeValueException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
@Service
public class SurveyResponsesServiceImpl implements SurveyResponsesService {
    private final SurveyParticipationRepository surveyParticipationRepository;
    private final SurveyRepository surveyRepository;
    private final OptionRepository optionRepository;
    private final QuestionRepository questionRepository;
    private final ClaimsPrincipalServiceImpl claimsPrincipalServiceImpl;
    private final IdentityUserRepository identityUserRepository;
    private final ModelMapper modelMapper;
    private final EntityManager entityManager;


    @Autowired
    public SurveyResponsesServiceImpl(
            SurveyParticipationRepository surveyParticipationRepository,
            SurveyRepository surveyRepository,
            OptionRepository optionRepository,
            QuestionRepository questionRepository,
            ClaimsPrincipalServiceImpl claimsPrincipalServiceImpl,
            IdentityUserRepository identityUserRepository,
            ModelMapper modelMapper,
            EntityManager entityManager) {
        this.surveyParticipationRepository = surveyParticipationRepository;
        this.surveyRepository = surveyRepository;
        this.optionRepository = optionRepository;
        this.questionRepository = questionRepository;
        this.claimsPrincipalServiceImpl = claimsPrincipalServiceImpl;
        this.identityUserRepository = identityUserRepository;
        this.modelMapper = modelMapper;
        this.entityManager = entityManager;
    }
    private IdentityUser findIdentityUserFromToken(String token) {
        String usernameFromJwt = claimsPrincipalServiceImpl.getCurrentUsernameIfExists(token);
        return identityUserRepository.findByUsername(usernameFromJwt)
                .orElseThrow(() -> new IllegalArgumentException("Invalid respondent ID - respondent doesn't exist"));
    }

    private Survey findSurveyById(UUID surveyId) {
        return surveyRepository.findById(surveyId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid survey ID - survey doesn't exist"));
    }

    private List<Question> findQuestionsByIds(List<UUID> questionIds, UUID surveyId) {
        return questionRepository.findAllByIds(surveyId, questionIds);
    }

    private List<Option> findOptionsByIds(List<UUID> optionIds, UUID questionId) {
        return optionRepository.findAllByIds(questionId, optionIds);
    }

    private SurveyParticipation saveSurveyParticipation(SendSurveyResponseDto sendSurveyResponseDto, IdentityUser identityUser, Survey survey) {
        SurveyParticipation surveyParticipation = new SurveyParticipation();
        surveyParticipation.setIdentityUser(identityUser);
        surveyParticipation.setDate(new Date());
        surveyParticipation.setSurvey(survey);
        return surveyParticipationRepository.save(surveyParticipation);
    }

private SurveyParticipation mapQuestionAnswers(SendSurveyResponseDto sendSurveyResponseDto, SurveyParticipation surveyParticipation, Survey survey) throws InvalidAttributeValueException {
    List<UUID> questionIds = sendSurveyResponseDto.getAnswers().stream()
            .map(AnswerDto::getQuestionId)
            .collect(Collectors.toList());

    List<Question> questions = findQuestionsByIds(questionIds, survey.getId());
    Map<UUID, Question> questionMap = questions.stream()
            .collect(Collectors.toMap(Question::getId, question -> question));

    List<QuestionAnswer> questionAnswers = sendSurveyResponseDto.getAnswers().stream()
            .map(answerDto -> {
                Question question = questionMap.get(answerDto.getQuestionId());
                if (question == null) {
                    throw new IllegalArgumentException("Invalid question ID - this question is not part of the survey: " + survey.getId());
                }
                QuestionAnswer questionAnswer = new QuestionAnswer();
                questionAnswer.setSurveyParticipation(surveyParticipation);
                questionAnswer.setQuestion(question);

                if (question.getQuestionType().equals(QuestionType.discrete_number_selection)) {
                    Integer numericAnswer = answerDto.getNumericAnswer();
                    if (numericAnswer == null || numericAnswer < question.getNumberRange().getFrom() || numericAnswer > question.getNumberRange().getTo() ) {

                        throw new IllegalArgumentException("Invalid Numeric answer.");
                    }
                    questionAnswer.setNumericAnswer(numericAnswer);
                } else {
                    List<UUID> optionIds = answerDto.getSelectedOptions().stream()
                            .map(SelectedOptionDto::getOptionId)
                            .collect(Collectors.toList());

                    List<Option> options = findOptionsByIds(optionIds, question.getId());
                    Map<UUID, Option> optionMap = options.stream()
                            .collect(Collectors.toMap(Option::getId, option -> option));

                    List<OptionSelection> optionSelections = answerDto.getSelectedOptions().stream()
                            .map(selectedOptionDto -> {
                                Option option = optionMap.get(selectedOptionDto.getOptionId());
                                if (option == null) {
                                    throw new IllegalArgumentException("Invalid option ID - this option is not part of the question: " + question.getId());
                                }
                                OptionSelection optionSelection = new OptionSelection();
                                optionSelection.setQuestionAnswer(questionAnswer);
                                optionSelection.setOption(option);
                                return optionSelection;
                            }).collect(Collectors.toList());
                    questionAnswer.setOptionSelections(optionSelections);
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
        IdentityUser identityUser = findIdentityUserFromToken(token);
        Survey survey = findSurveyById(sendSurveyResponseDto.getSurveyId());
        SurveyParticipation surveyParticipation = saveSurveyParticipation(sendSurveyResponseDto, identityUser, survey);
        SurveyParticipation finalSurveyParticipation = mapQuestionAnswers(sendSurveyResponseDto, surveyParticipation, survey);
        return mapToDto(finalSurveyParticipation, sendSurveyResponseDto, identityUser);
    }
}

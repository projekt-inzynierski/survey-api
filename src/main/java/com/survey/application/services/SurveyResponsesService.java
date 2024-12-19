package com.survey.application.services;

import com.survey.application.dtos.SurveyResultDto;
import com.survey.application.dtos.surveyDtos.SendOfflineSurveyResponseDto;
import com.survey.application.dtos.surveyDtos.SendOnlineSurveyResponseDto;
import com.survey.application.dtos.surveyDtos.SurveyParticipationDto;

import javax.management.InvalidAttributeValueException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface SurveyResponsesService {
    SurveyParticipationDto saveSurveyResponseOnline(SendOnlineSurveyResponseDto sendOnlineSurveyResponseDto) throws InvalidAttributeValueException;
    List<SurveyParticipationDto> saveSurveyResponsesOffline(List<SendOfflineSurveyResponseDto> sendOfflineSurveyResponseDtoList);
    List<SurveyResultDto> getSurveyResults(UUID surveyId, UUID identityUserId, OffsetDateTime dateFrom, OffsetDateTime dateTo);
}

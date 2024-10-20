package com.survey.application.services;

import com.survey.application.dtos.surveyDtos.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface SurveyService {

    ResponseSurveyDto createSurvey(CreateSurveyDto createSurveyDto);
    List<ResponseSurveyDto> getSurveysByCompletionDate(LocalDate completionDate);
    List<ResponseSurveyShortDto> getSurveysShort();
    List<ResponseSurveyShortSummariesDto> getSurveysShortSummaries();
    ResponseSurveyDto getSurveyById(UUID surveyId);
    List<ResponseSurveyWithTimeSlotsDto> getallSurveysWithTimeSlots();
}

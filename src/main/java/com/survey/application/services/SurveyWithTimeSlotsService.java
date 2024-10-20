package com.survey.application.services;

import com.survey.application.dtos.surveyDtos.ResponseSurveyWithTimeSlotsDto;
import com.survey.domain.models.Survey;

import java.util.List;

public interface SurveyWithTimeSlotsService {
    public List<ResponseSurveyWithTimeSlotsDto> getSurveysWithTimeSlots(List<Survey> surveys);
}

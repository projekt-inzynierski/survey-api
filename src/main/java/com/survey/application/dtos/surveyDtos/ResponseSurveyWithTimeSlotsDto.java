package com.survey.application.dtos.surveyDtos;

import com.survey.application.dtos.SurveySendingPolicyTimesDto;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ResponseSurveyWithTimeSlotsDto {
    private ResponseSurveyDto survey;
    private List<SurveySendingPolicyTimesDto> surveySendingPolicyTimes;
}

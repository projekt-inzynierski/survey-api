package com.survey.application.services;

import com.survey.application.dtos.SurveySendingPolicyTimesDto;
import com.survey.application.dtos.surveyDtos.ResponseSurveyDto;
import com.survey.application.dtos.surveyDtos.ResponseSurveyWithTimeSlotsDto;
import com.survey.domain.models.Survey;
import com.survey.domain.models.SurveySendingPolicy;
import com.survey.domain.repository.SurveySendingPolicyRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SurveyWithTimeSlotsServiceImpl implements SurveyWithTimeSlotsService{
    private final SurveySendingPolicyRepository surveySendingPolicyRepository;
    private final UpcomingTimeSlotsService upcomingTimeSlotsService;
    private final ModelMapper modelMapper;

    @Autowired
    public SurveyWithTimeSlotsServiceImpl(SurveySendingPolicyRepository surveySendingPolicyRepository, UpcomingTimeSlotsService upcomingTimeSlotsService, ModelMapper modelMapper) {
        this.surveySendingPolicyRepository = surveySendingPolicyRepository;
        this.upcomingTimeSlotsService = upcomingTimeSlotsService;
        this.modelMapper = modelMapper;
    }

    @Override
    public List<ResponseSurveyWithTimeSlotsDto> getSurveysWithTimeSlots(List<Survey> surveys) {
        List<ResponseSurveyWithTimeSlotsDto> responseDtoList = new ArrayList<>();

        for (Survey survey : surveys){
            List<SurveySendingPolicy> sendingPolicies = surveySendingPolicyRepository.findAllBySurveyId(survey.getId());

            if (!sendingPolicies.isEmpty()) {
                List<SurveySendingPolicyTimesDto> validTimeSlots = upcomingTimeSlotsService.getUpcomingTimeSlots(sendingPolicies);
                if (!validTimeSlots.isEmpty()) {
                    ResponseSurveyWithTimeSlotsDto responseDto = createResponseDto(survey, validTimeSlots);
                    responseDtoList.add(responseDto);
                }
            }
        }

        return responseDtoList;
    }

    private ResponseSurveyWithTimeSlotsDto createResponseDto(Survey survey, List<SurveySendingPolicyTimesDto> validTimeslots){
        ResponseSurveyWithTimeSlotsDto responseDto = new ResponseSurveyWithTimeSlotsDto();
        responseDto.setSurvey(modelMapper.map(survey, ResponseSurveyDto.class));
        responseDto.setSurveySendingPolicyTimes(validTimeslots);
        return responseDto;
    }
}

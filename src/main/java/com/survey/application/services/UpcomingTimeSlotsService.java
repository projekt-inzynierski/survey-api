package com.survey.application.services;

import com.survey.application.dtos.SurveySendingPolicyTimesDto;
import com.survey.domain.models.SurveySendingPolicy;

import java.util.List;

public interface UpcomingTimeSlotsService {
    List<SurveySendingPolicyTimesDto> getUpcomingTimeSlots(List<SurveySendingPolicy> sendingPolicies);
}

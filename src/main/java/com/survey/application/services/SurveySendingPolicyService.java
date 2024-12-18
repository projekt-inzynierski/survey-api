package com.survey.application.services;

import com.survey.application.dtos.*;
import org.apache.coyote.BadRequestException;

import javax.management.BadAttributeValueExpException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.InvalidAttributeValueException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

public interface SurveySendingPolicyService {
    SurveySendingPolicyDto createSurveySendingPolicy(CreateSurveySendingPolicyDto createSurveySendingPolicyDto) throws InstanceAlreadyExistsException, NoSuchElementException, IllegalArgumentException, BadRequestException, BadAttributeValueExpException, InstanceNotFoundException, InvalidAttributeValueException;
    List<SurveySendingPolicyDto> getSurveysSendingPolicyById(UUID surveyId);
    List<SurveySendingPolicyTimesDto> deleteTimeSlotsByIds(TimeSlotsToDeleteDto timeSlotsToDelete);

}

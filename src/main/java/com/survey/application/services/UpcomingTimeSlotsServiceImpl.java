package com.survey.application.services;

import com.survey.application.dtos.SurveySendingPolicyTimesDto;
import com.survey.domain.models.*;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UpcomingTimeSlotsServiceImpl implements UpcomingTimeSlotsService {

    private final ModelMapper modelMapper;

    @Autowired
    public UpcomingTimeSlotsServiceImpl(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    @Override
    public List<SurveySendingPolicyTimesDto> getUpcomingTimeSlots(List<SurveySendingPolicy> sendingPolicies) {
        return sendingPolicies.stream()
                .flatMap(policy -> policy.getTimeSlots().stream())
                .filter(this::isTimeSlotInTheFuture)
                .sorted(Comparator.comparing(SurveyParticipationTimeSlot::getStart))
                .map(this::mapToTimeSlotDto)
                .collect(Collectors.toList());
    }

    private boolean isTimeSlotInTheFuture(SurveyParticipationTimeSlot slot){
        OffsetDateTime currentTimeInUTC = OffsetDateTime.now(ZoneOffset.UTC);
        return slot.getFinish().isAfter(currentTimeInUTC);
    }

    private SurveySendingPolicyTimesDto mapToTimeSlotDto(SurveyParticipationTimeSlot slot){
        return modelMapper.map(slot, SurveySendingPolicyTimesDto.class);
    }
}

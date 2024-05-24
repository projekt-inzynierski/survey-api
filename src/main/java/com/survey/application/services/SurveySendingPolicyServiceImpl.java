package com.survey.application.services;

import com.survey.application.dtos.CreateSurveySendingPolicyDto;
import com.survey.application.dtos.SurveySendingPolicyDto;
import com.survey.domain.models.*;
import com.survey.domain.repository.SurveyRepository;
import com.survey.domain.repository.SurveySendingPolicyRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.management.InvalidAttributeValueException;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SurveySendingPolicyServiceImpl implements SurveySendingPolicyService {

    @Autowired
    private SurveySendingPolicyRepository surveySendingPolicyRepository;

    @Autowired
    private SurveyRepository surveyRepository;

    @Autowired
    private TimeSlotsValidationService timeSlotsValidationService;

    @Autowired
    private ModelMapper modelMapper;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public SurveySendingPolicyDto createSurveySendingPolicy(CreateSurveySendingPolicyDto dto) throws InvalidAttributeValueException {

        SurveySendingPolicy surveySendingPolicy = new SurveySendingPolicy();
        UUID currentSurveyId = dto.getSurveyId();

        surveySendingPolicy.setSurvey(surveyRepository.findById(currentSurveyId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid survey ID - survey doesn't exist")));

        timeSlotsValidationService.validateTimeSlots(dto);

        surveySendingPolicy.setTimeSlots(
                dto.getSurveyParticipationTimeSlots().stream()
                        .map(slotDto -> {
                            SurveyParticipationTimeSlot slot = new SurveyParticipationTimeSlot();
                            slot.setStart(slotDto.getStart());
                            slot.setFinish(slotDto.getFinish());
                            slot.setSurveySendingPolicy(surveySendingPolicy);
                            return slot;
                        }).collect(Collectors.toList())
        );
        SurveySendingPolicy saveSurveySendingPolicy = surveySendingPolicyRepository.saveAndFlush(surveySendingPolicy);
        entityManager.refresh(saveSurveySendingPolicy);
        SurveySendingPolicyDto surveySendingPolicyDto = modelMapper.map(saveSurveySendingPolicy, SurveySendingPolicyDto.class);
        surveySendingPolicyDto.setSurveyId(currentSurveyId);

        return surveySendingPolicyDto;
    }
}
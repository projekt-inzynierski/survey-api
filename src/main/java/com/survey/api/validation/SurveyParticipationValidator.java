package com.survey.api.validation;

import com.survey.application.services.ClaimsPrincipalService;
import com.survey.domain.repository.SurveyParticipationRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.security.authentication.BadCredentialsException;

import java.util.UUID;

public class SurveyParticipationValidator implements ConstraintValidator<ValidSurveyParticipationId, UUID> {
    private final SurveyParticipationRepository surveyParticipationRepository;
    private final ClaimsPrincipalService claimsPrincipalService;

    public SurveyParticipationValidator(SurveyParticipationRepository surveyParticipationRepository, ClaimsPrincipalService claimsPrincipalService) {
        this.surveyParticipationRepository = surveyParticipationRepository;
        this.claimsPrincipalService = claimsPrincipalService;
    }

    @Override
    public boolean isValid(UUID surveyParticipationId, ConstraintValidatorContext constraintValidatorContext) {
        try {
            UUID respondentId = claimsPrincipalService.findIdentityUser().getId();
            return surveyParticipationId == null || validateSurveyParticipationId(surveyParticipationId, respondentId);
        } catch (BadCredentialsException e){
            return false;
        }
    }

    private boolean validateSurveyParticipationId(UUID surveyParticipationId, UUID respondentId){
        return surveyParticipationRepository.findByIdAndIdentityUserId(surveyParticipationId, respondentId).isPresent();
    }
}

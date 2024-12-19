package com.survey.api.controllers;

import com.survey.api.security.Role;
import com.survey.application.dtos.SurveyResultDto;
import com.survey.application.dtos.surveyDtos.SendOfflineSurveyResponseDto;
import com.survey.application.dtos.surveyDtos.SendOnlineSurveyResponseDto;
import com.survey.application.dtos.surveyDtos.SurveyParticipationDto;
import com.survey.application.services.ClaimsPrincipalService;
import com.survey.application.services.SurveyResponsesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.annotation.RequestScope;

import javax.management.InvalidAttributeValueException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping("/api/surveyresponses")
@RequestScope
public class SurveyResponsesController {
    private final SurveyResponsesService surveyResponsesService;
    private final ClaimsPrincipalService claimsPrincipalService;

    @Autowired
    public SurveyResponsesController(SurveyResponsesService surveyResponsesService, ClaimsPrincipalService claimsPrincipalService){
        this.surveyResponsesService = surveyResponsesService;
        this.claimsPrincipalService = claimsPrincipalService;
    }

    @PostMapping
    public ResponseEntity<SurveyParticipationDto> saveSurveyResponseOnline(@Validated @RequestBody SendOnlineSurveyResponseDto sendOnlineSurveyResponseDto) throws InvalidAttributeValueException {
        claimsPrincipalService.ensureRole(Role.RESPONDENT.getRoleName());
        SurveyParticipationDto surveyParticipationDto = surveyResponsesService.saveSurveyResponseOnline(sendOnlineSurveyResponseDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(surveyParticipationDto);
    }

    @PostMapping("/offline")
    public ResponseEntity<List<SurveyParticipationDto>> saveSurveyResponseOffline(@RequestBody List<SendOfflineSurveyResponseDto> sendOfflineSurveyResponseDtoList){
        claimsPrincipalService.ensureRole(Role.RESPONDENT.getRoleName());
        List<SurveyParticipationDto> surveyParticipationDtoList = surveyResponsesService.saveSurveyResponsesOffline(sendOfflineSurveyResponseDtoList);
        return ResponseEntity.status(HttpStatus.CREATED).body(surveyParticipationDtoList);
    }

    @GetMapping("/results")
    public ResponseEntity<List<SurveyResultDto>> getSurveyResults(
            @RequestParam(value = "surveyId", required = false) UUID surveyId,
            @RequestParam(value = "respondentId", required = false) UUID identityUserId,
            @RequestParam(value = "dateFrom", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'") OffsetDateTime dateFrom,
            @RequestParam(value = "dateTo", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'") OffsetDateTime dateTo) {

        claimsPrincipalService.ensureRole(Role.ADMIN.getRoleName());
        List<SurveyResultDto> results = surveyResponsesService.getSurveyResults(surveyId, identityUserId, dateFrom, dateTo);
        return ResponseEntity.status(HttpStatus.OK).body(results);
    }
}

package com.survey.api.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.survey.application.dtos.surveyDtos.*;
import com.survey.application.services.SurveyService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/surveys")
@CrossOrigin
public class SurveyController {
    private final SurveyService surveyService;
    private final ObjectMapper objectMapper;

    @Autowired
    public SurveyController(SurveyService surveyService, ObjectMapper objectMapper) {
        this.surveyService = surveyService;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public ResponseEntity<ResponseSurveyDto> createSurvey(@RequestPart("json") @Validated String createSurveyDto, @RequestPart(value = "files", required = false) List<MultipartFile> files) throws JsonProcessingException {
        CreateSurveyDto surveyDto = objectMapper.readValue(createSurveyDto, CreateSurveyDto.class);
        ResponseSurveyDto responseDto = surveyService.createSurvey(surveyDto, files);
        return ResponseEntity.status(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body(responseDto);
    }

    @GetMapping(params = "completionDate")
    public ResponseEntity<List<ResponseSurveyDto>> getSurveysByCompletionDate(
            @RequestParam("completionDate") @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate completionDate) {
        List<ResponseSurveyDto> surveys = surveyService.getSurveysByCompletionDate(completionDate);
        return ResponseEntity.ok(surveys);
    }

    @GetMapping(params = "surveyId")
    public ResponseEntity<ResponseSurveyDto> getSurveyById(@RequestParam("surveyId") UUID surveyId){
        ResponseSurveyDto responseSurveyDto = surveyService.getSurveyById(surveyId);
        return ResponseEntity.ok(responseSurveyDto);
    }

    @GetMapping("/short")
    public ResponseEntity<List<ResponseSurveyShortDto>> getShortSurveys(){
        List<ResponseSurveyShortDto> shortSurveys = surveyService.getSurveysShort();
        return ResponseEntity.status(HttpStatus.OK).body(shortSurveys);
    }

    @GetMapping("/shortsummaries")
    @CrossOrigin
    public ResponseEntity<List<ResponseSurveyShortSummariesDto>> getShortSurveysSummaries(){
        List<ResponseSurveyShortSummariesDto> shortSummariesSurveys = surveyService.getSurveysShortSummaries();
        return ResponseEntity.status(HttpStatus.OK).body(shortSummariesSurveys);
    }

    @CrossOrigin
    @GetMapping("/allwithtimeslots")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<ResponseSurveyWithTimeSlotsDto>> getAllSurveysWithTimeSlots(){
        List<ResponseSurveyWithTimeSlotsDto> responseSurveyWithTimeSlotsDtoList = surveyService.getAllSurveysWithTimeSlots();
        return ResponseEntity.status(HttpStatus.OK).body(responseSurveyWithTimeSlotsDtoList);
    }


}

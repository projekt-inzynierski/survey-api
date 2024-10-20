package com.survey.api.controllers;

import com.survey.application.dtos.surveyDtos.*;
import com.survey.application.services.SurveyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/surveys")
@CrossOrigin
public class SurveyController {

    private final SurveyService surveyService;

    @Autowired
    public SurveyController(SurveyService surveyService) {
        this.surveyService = surveyService;
    }


    @PostMapping
    public ResponseEntity<ResponseSurveyDto> createSurvey(@Validated @RequestBody CreateSurveyDto createSurveyDto){
        ResponseSurveyDto responseDto = surveyService.createSurvey(createSurveyDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
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
    public ResponseEntity<List<ResponseSurveyWithTimeSlotsDto>> getAllSurveysWithTimeSlots(){
        List<ResponseSurveyWithTimeSlotsDto> responseSurveyWithTimeSlotsDtoList = surveyService.getallSurveysWithTimeSlots();
        return ResponseEntity.status(HttpStatus.OK).body(responseSurveyWithTimeSlotsDtoList);
    }



}

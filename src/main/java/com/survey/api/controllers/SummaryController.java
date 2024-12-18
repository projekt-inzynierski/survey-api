package com.survey.api.controllers;

import com.survey.api.security.Role;
import com.survey.application.dtos.HistogramDataDto;
import com.survey.application.services.ClaimsPrincipalService;
import com.survey.application.services.SummaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/summaries")
public class SummaryController {
    private final SummaryService summaryService;
    private final ClaimsPrincipalService claimsPrincipalService;

    @Autowired
    public SummaryController(SummaryService summaryService, ClaimsPrincipalService claimsPrincipalService) {
        this.summaryService = summaryService;
        this.claimsPrincipalService = claimsPrincipalService;
    }


    @GetMapping("/histogram")
    public ResponseEntity<List<HistogramDataDto>> getHistogramData(
            @RequestParam("surveyId") UUID surveyId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date date){

        claimsPrincipalService.ensureRole(Role.ADMIN.getRoleName());

        List<HistogramDataDto> histogramDataDtoList = summaryService.getHistogramData(surveyId, date);
        return ResponseEntity.ok(histogramDataDtoList);
    }
}

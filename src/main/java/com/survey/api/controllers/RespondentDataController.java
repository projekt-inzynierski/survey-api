package com.survey.api.controllers;

import com.survey.application.dtos.CreateRespondentDataDto;
import com.survey.application.dtos.RespondentDataDto;
import com.survey.application.services.RespondentDataService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InvalidAttributeValueException;
import java.util.List;

@RestController
@RequestMapping("/api/respondents")
public class RespondentDataController {

    private final RespondentDataService respondentDataService;

    @Autowired
    public RespondentDataController(RespondentDataService respondentDataService){
            this.respondentDataService = respondentDataService;
    }

    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<RespondentDataDto> createRespondent(@Validated @RequestBody CreateRespondentDataDto dto,
                                              @RequestHeader(value="Authorization", required = false) String token
    ) throws BadRequestException, InvalidAttributeValueException, InstanceAlreadyExistsException {
        RespondentDataDto createdRespondentData = respondentDataService.createRespondent(dto, token);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdRespondentData);
    }

    @GetMapping("/all")
    public ResponseEntity<List<RespondentDataDto>> getAll(){
        List<RespondentDataDto> respondents = respondentDataService.getAll();
        return ResponseEntity.status(HttpStatus.OK).body(respondents);
    }

    @GetMapping
    public ResponseEntity<RespondentDataDto> getFromUserContext(){
        RespondentDataDto respondent = respondentDataService.getFromUserContext();
        return ResponseEntity.status(HttpStatus.OK).body(respondent);
    }
}

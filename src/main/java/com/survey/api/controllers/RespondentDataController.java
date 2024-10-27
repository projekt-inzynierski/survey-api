package com.survey.api.controllers;

import com.survey.application.dtos.CreateRespondentDataDto;
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
import java.util.Map;

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
    public ResponseEntity<Map<String, Object>> createRespondent(@Validated @RequestBody List<CreateRespondentDataDto> dto,
                                                                @RequestHeader(value="Authorization", required = false) String token) throws BadRequestException, InvalidAttributeValueException, InstanceAlreadyExistsException {
        Map<String, Object> response = respondentDataService.createRespondent(dto, token);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/all")
    public ResponseEntity<List<Map<String, Object>>> getAll(){
        List<Map<String, Object>> response = respondentDataService.getAll();
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getFromUserContext(){
        Map<String, Object> response = respondentDataService.getFromUserContext();
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}

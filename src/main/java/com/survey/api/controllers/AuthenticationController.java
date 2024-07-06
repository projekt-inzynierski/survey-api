package com.survey.api.controllers;

import com.survey.application.dtos.CreateRespondentsAccountsDto;
import com.survey.application.dtos.LoginDto;
import com.survey.application.services.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/authentication")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @Autowired
    public AuthenticationController(AuthenticationService authenticationService){
        this.authenticationService = authenticationService;
    }

    @PostMapping("/login")
    public String login(@Validated @RequestBody LoginDto loginDto){
        return authenticationService.getJwtToken(loginDto);
    }

    @PostMapping("/respondents")
    @CrossOrigin
    public List<LoginDto> createRespondentsAccounts(@Validated @RequestBody CreateRespondentsAccountsDto dto){
        return authenticationService.createRespondentsAccounts(dto);
    }
}

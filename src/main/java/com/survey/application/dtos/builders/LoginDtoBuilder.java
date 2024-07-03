package com.survey.application.dtos.builders;

import com.survey.application.dtos.LoginDto;

public class LoginDtoBuilder {
    private String username;
    private String password;

    public LoginDtoBuilder withUsername(String username){
        this.username = username;
        return this;
    }

    public LoginDtoBuilder withPassword(String password){
        this.password = password;
        return this;
    }

    public LoginDto build(){
        return new LoginDto(username, password);
    }
}

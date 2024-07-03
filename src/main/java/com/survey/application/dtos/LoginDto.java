package com.survey.application.dtos;

import com.survey.application.dtos.builders.LoginDtoBuilder;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LoginDto {
    @NotEmpty
    private String username;
    @NotEmpty
    private String password;

    public LoginDto(String username, String password){
        this.username = username;
        this.password = password;
    }

    public static LoginDtoBuilder builder(){
        return new LoginDtoBuilder();
    }
}

package com.survey.application.dtos.surveyDtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SurveyDto {

    @NotBlank
    @Size(max = 100)
    private String name;
}

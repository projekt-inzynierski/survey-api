package com.survey.application.dtos.initialSurvey;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreateInitialSurveyOptionDto {
    @NotNull
    private Integer order;
    @NotNull
    private String content;
}

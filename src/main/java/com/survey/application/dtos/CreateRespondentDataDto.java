package com.survey.application.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class CreateRespondentDataDto {
    @NotNull
    private UUID surveyId;
    @NotEmpty
    private List<@Valid RespondentDataAnswerDto> answers;
}

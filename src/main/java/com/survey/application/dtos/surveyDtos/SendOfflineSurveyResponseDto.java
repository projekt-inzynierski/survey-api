package com.survey.application.dtos.surveyDtos;

import com.survey.api.validation.ValidSendSurveyResponse;
import com.survey.application.dtos.SensorDataDto;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ValidSendSurveyResponse
@Accessors(chain = true)
public class SendOfflineSurveyResponseDto implements SendSurveyResponseDto {
    @NotNull
    private UUID surveyId;
    @NotNull
    private OffsetDateTime startDate;
    @NotNull
    private OffsetDateTime finishDate;
    private SensorDataDto sensorData;
    @NotNull
    private List<AnswerDto> answers;
}

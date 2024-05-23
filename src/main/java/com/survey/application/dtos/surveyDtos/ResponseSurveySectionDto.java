package com.survey.application.dtos.surveyDtos;

import com.survey.domain.models.enums.Visibility;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class ResponseSurveySectionDto {
    private UUID id;
    private Integer order;
    private String name;
    private Visibility visibility;
    private String groupId;
    private Long rowVersion;

    private List<ResponseQuestionDto> questions;

}
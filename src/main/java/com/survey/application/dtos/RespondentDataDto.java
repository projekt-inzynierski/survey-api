package com.survey.application.dtos;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class RespondentDataDto {
    private UUID id;
    private UUID identityUserId;
    private String gender;
    private Integer ageCategoryId;
    private Integer occupationCategoryId;
    private Integer educationCategoryId;
    private Integer greeneryAreaCategoryId;
    private Integer medicationUseId;
    private Integer healthConditionId;
    private Integer stressLevelId;
    private Integer lifeSatisfactionId;
    private Integer qualityOfSleepId;

}
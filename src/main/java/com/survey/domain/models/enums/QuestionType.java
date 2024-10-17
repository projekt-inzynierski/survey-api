package com.survey.domain.models.enums;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum QuestionType {
    single_text_selection(0),
    discrete_number_selection(1),
    yes_no_selection(2),
    multiple_choice(3),
    number_selection(4);
    private final int value;

    QuestionType(int value) {
        this.value = value;
    }

    public static QuestionType fromValue(int value) {
        return Arrays.stream(QuestionType.values())
                .filter(questionType -> questionType.value == value)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown enum value: " + value));
    }
}

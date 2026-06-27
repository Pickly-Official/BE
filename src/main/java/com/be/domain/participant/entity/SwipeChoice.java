package com.be.domain.participant.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum SwipeChoice {
    LIKE, SKIP;

    @JsonCreator
    public static SwipeChoice from(String value) {
        return SwipeChoice.valueOf(value.toUpperCase());
    }
}

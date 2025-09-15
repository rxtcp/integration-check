package io.github.nety.integrationcheck.domain;

import lombok.Getter;

@Getter
public enum CheckType {
    REST_API, DATABASE, KAFKA
}
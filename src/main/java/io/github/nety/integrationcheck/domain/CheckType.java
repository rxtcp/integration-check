package io.github.nety.integrationcheck.domain;

import lombok.Getter;

@Getter
public enum CheckType {
    HTTP("HTTP");

    private final String code;

    CheckType(String code) {
        this.code = code;
    }

    public static CheckType fromCode(String code) {
        for (CheckType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Неизвестный тип проверки: " + code);
    }
}
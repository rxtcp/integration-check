package io.github.rxtcp.integrationcheck.domain;

import lombok.Getter;

/**
 * Тип проверки интеграции (дискриминатор).
 */
@Getter
public enum CheckType {
    /**
     * Проверка REST API.
     */
    REST_API
}

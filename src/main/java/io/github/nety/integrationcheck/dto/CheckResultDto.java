package io.github.nety.integrationcheck.dto;

import io.github.nety.integrationcheck.enums.CheckRunStatus;
import io.github.nety.integrationcheck.enums.FailureReason;
import jakarta.validation.constraints.NotNull;

/**
 * Результат выполнения проверки.
 *
 * @param status        обязательный статус
 * @param failureReason причина неуспеха (для {@code SUCCEEDED} обычно {@code null})
 * @param details       детали ответа/ошибки
 */
public record CheckResultDto(
        @NotNull CheckRunStatus status,
        FailureReason failureReason,
        String details
) {
}

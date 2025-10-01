package io.github.rxtcp.integrationcheck.dto;

import io.github.rxtcp.integrationcheck.enums.CheckType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * DTO проверки.
 *
 * @param id             идентификатор
 * @param name           имя (обязательное)
 * @param description    описание
 * @param enabled        активна
 * @param runIntervalMin интервал, мин (1–10080)
 * @param nextRunAt      время следующего запуска
 * @param type           тип проверки
 * @param profile        профиль проверки
 */
public record CheckDto(
        Long id,
        @NotBlank String name,
        String description,
        boolean enabled,
        @Min(1) @Max(10080) int runIntervalMin,
        @NotNull LocalDateTime nextRunAt,
        @NotNull CheckType type,
        @NotNull CheckProfileDto profile
) {
}

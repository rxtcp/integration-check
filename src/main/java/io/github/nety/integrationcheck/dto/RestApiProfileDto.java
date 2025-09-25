package io.github.nety.integrationcheck.dto;

import io.github.nety.integrationcheck.enums.HttpMethod;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.URL;

/**
 * DTO профиля REST API.
 *
 * @param checkId          идентификатор проверки (владелец)
 * @param profileId        идентификатор профиля (nullable при создании)
 * @param url              целевой URL
 * @param httpMethod       HTTP-метод
 * @param timeoutSeconds   таймаут, сек (1–600)
 * @param headers          заголовки в JSON: {"Header":["v1","v2"]}
 * @param requestBody      тело запроса (опционально)
 * @param expectedHttpCode ожидаемый HTTP-код (100–599)
 */
public record RestApiProfileDto(
        @NotNull Long checkId,
        Long profileId,
        @NotBlank @URL String url,
        @NotNull HttpMethod httpMethod,
        @Min(1) @Max(600) int timeoutSeconds,
        String headers,
        String requestBody,
        @Min(100) @Max(599) int expectedHttpCode
) implements CheckProfileDto {
}

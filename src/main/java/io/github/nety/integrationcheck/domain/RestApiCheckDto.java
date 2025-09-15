package io.github.nety.integrationcheck.domain;

import java.time.LocalDateTime;
import java.util.Map;

public record RestApiCheckDto(
        Long id,
        String name,
        String description,
        boolean enabled,
        int runIntervalMinutes,
        LocalDateTime nextRunAt,
        String url,
        HttpMethod httpMethod,
        int timeoutSeconds,
        Map<String, String> headers,
        String requestBody
) implements CheckDto {
}

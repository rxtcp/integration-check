package io.github.rxtcp.integrationcheck.dto;

import io.github.rxtcp.integrationcheck.enums.CheckType;
import io.github.rxtcp.integrationcheck.enums.HttpMethod;

import java.time.LocalDateTime;

/**
 * Набор тестовых фикстур для DTO.
 * <p>
 * Подход:
 * - Значения по умолчанию выведены в константы: меньше «магии» в тестах, проще менять.
 * - Время детерминировано (см. {@link #DEFAULT_NEXT_RUN_AT}), чтобы исключить флапающие проверки.
 */
final class TestFixtures {

    // ===== Значения по умолчанию (говорящие имена) =====
    static final long DEFAULT_CHECK_ID = 1L;
    static final Long DEFAULT_PROFILE_ID = 10L; // может быть null в сценарии "создания"
    static final String DEFAULT_CHECK_NAME = "Ping API";
    static final String DEFAULT_CHECK_DESCRIPTION = "Simple healthcheck";
    static final boolean DEFAULT_ENABLED = true;
    static final int DEFAULT_RUN_INTERVAL_MIN = 5;
    static final LocalDateTime DEFAULT_NEXT_RUN_AT = LocalDateTime.of(2030, 1, 1, 12, 0);
    static final String DEFAULT_URL = "https://example.org/health";
    static final int DEFAULT_TIMEOUT_SECONDS = 5;
    static final String DEFAULT_HEADERS_JSON = "{\"X-Trace-Id\":[\"abc\"]}";
    static final String DEFAULT_REQUEST_BODY = null;
    static final int DEFAULT_EXPECTED_HTTP_CODE = 200;
    private TestFixtures() {
        // утилитарный класс
    }

    // ===== Утилиты выбора любого значения enum (не привязаны к конкретному имени константы) =====
    static CheckType anyCheckType() {
        return CheckType.values()[0];
    }

    static HttpMethod anyHttpMethod() {
        return HttpMethod.values()[0];
    }

    // ===== Фабрики RestApiProfileDto =====

    /**
     * Валидный профиль REST API для указанного {@code checkId} с дефолтами.
     */
    static RestApiProfileDto validRestApiProfile(long checkId) {
        return validRestApiProfile(checkId, DEFAULT_PROFILE_ID);
    }

    /**
     * Валидный профиль REST API с возможностью задать {@code profileId} (может быть null для сценариев create).
     */
    static RestApiProfileDto validRestApiProfile(long checkId, Long profileId) {
        return restApiProfile(
                checkId,
                profileId,
                DEFAULT_URL,
                anyHttpMethod(),
                DEFAULT_TIMEOUT_SECONDS,
                DEFAULT_HEADERS_JSON,
                DEFAULT_REQUEST_BODY,
                DEFAULT_EXPECTED_HTTP_CODE
        );
    }

    /**
     * Базовая фабрика профиля REST API с полной кастомизацией.
     */
    static RestApiProfileDto restApiProfile(
            Long checkId,
            Long profileId,
            String url,
            HttpMethod httpMethod,
            int timeoutSeconds,
            String headersJson,
            String requestBody,
            int expectedHttpCode
    ) {
        return new RestApiProfileDto(
                checkId,
                profileId,
                url,
                httpMethod,
                timeoutSeconds,
                headersJson,
                requestBody,
                expectedHttpCode
        );
    }

    // ===== Фабрики CheckDto =====

    /**
     * Валидный CheckDto с детерминированным временем nextRunAt.
     */
    static CheckDto validCheckDto() {
        return validCheckDto(DEFAULT_CHECK_ID);
    }

    /**
     * Валидный CheckDto для заданного {@code id} с детерминированным временем nextRunAt.
     */
    static CheckDto validCheckDto(long id) {
        return checkDto(
                id,
                DEFAULT_CHECK_NAME,
                DEFAULT_CHECK_DESCRIPTION,
                DEFAULT_ENABLED,
                DEFAULT_RUN_INTERVAL_MIN,
                DEFAULT_NEXT_RUN_AT,
                anyCheckType(),
                validRestApiProfile(id)
        );
    }

    /**
     * Базовая фабрика CheckDto с полной кастомизацией.
     */
    static CheckDto checkDto(
            Long id,
            String name,
            String description,
            boolean enabled,
            int runIntervalMin,
            LocalDateTime nextRunAt,
            CheckType type,
            RestApiProfileDto profile
    ) {
        return new CheckDto(
                id,
                name,
                description,
                enabled,
                runIntervalMin,
                nextRunAt,
                type,
                profile
        );
    }
}
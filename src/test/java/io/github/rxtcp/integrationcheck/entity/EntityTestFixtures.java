package io.github.rxtcp.integrationcheck.entity;

import io.github.rxtcp.integrationcheck.domain.CheckType;
import io.github.rxtcp.integrationcheck.domain.HttpMethod;

import java.time.LocalDateTime;

/**
 * Набор тестовых фикстур для сущностей домена.
 * <p>
 * Подход:
 * - Все значения по умолчанию вынесены в читаемые константы — меньше «магии» в тестах.
 * - Время детерминировано (см. {@link #DEFAULT_NEXT_RUN_AT}) — исключаем флапающие проверки на CI.
 */
public final class EntityTestFixtures {

    // ===== Значения по умолчанию (говорящие имена) =====
    public static final String DEFAULT_CHECK_NAME = "Check";
    public static final String DEFAULT_CHECK_DESCRIPTION = "desc";
    public static final boolean DEFAULT_CHECK_ENABLED = true;
    public static final int DEFAULT_RUN_INTERVAL_MIN = 5;
    public static final LocalDateTime DEFAULT_NEXT_RUN_AT = LocalDateTime.of(2030, 1, 1, 12, 0);
    public static final String DEFAULT_URL = "https://example.org/health";
    public static final int DEFAULT_TIMEOUT_SECONDS = 5;
    public static final String DEFAULT_HEADERS_JSON = "{\"X-Trace-Id\":[\"abc\"]}";
    public static final String DEFAULT_REQUEST_BODY = null;
    public static final int DEFAULT_EXPECTED_HTTP_CODE = 200;
    private EntityTestFixtures() {
        // утилитарный класс
    }

    // ===== Утилиты выбора любого значения enum (не завязываемся на конкретные константы) =====
    public static CheckType anyCheckType() {
        return CheckType.values()[0];
    }

    public static HttpMethod anyHttpMethod() {
        return HttpMethod.values()[0];
    }

    // ===== Фабрики Check =====

    /**
     * Исходная фабрика (сохранена): создаёт валидный {@link Check} с указанным названием.
     * Использует детерминированное время {@link #DEFAULT_NEXT_RUN_AT}.
     */
    public static Check newCheck(String name) {
        return Check.builder()
                .name(name)
                .description(DEFAULT_CHECK_DESCRIPTION)
                .enabled(DEFAULT_CHECK_ENABLED)
                .runIntervalMin(DEFAULT_RUN_INTERVAL_MIN)
                .nextRunAt(DEFAULT_NEXT_RUN_AT)
                .type(anyCheckType())
                .build();
    }

    /**
     * Удобная перегрузка: валидный {@link Check} с названием по умолчанию.
     */
    public static Check newCheck() {
        return newCheck(DEFAULT_CHECK_NAME);
    }

    /**
     * Полностью настраиваемая фабрика {@link Check}.
     * Полезно, когда в тесте нужно явно указать конкретные значения.
     */
    public static Check check(
            String name,
            String description,
            boolean enabled,
            int runIntervalMin,
            LocalDateTime nextRunAt,
            CheckType type
    ) {
        return Check.builder()
                .name(name)
                .description(description)
                .enabled(enabled)
                .runIntervalMin(runIntervalMin)
                .nextRunAt(nextRunAt)
                .type(type)
                .build();
    }

    // ===== Фабрики RestApiProfile =====

    /**
     * Исходная фабрика (сохранена): валидный профиль REST API.
     */
    public static RestApiProfile newRestProfile() {
        final RestApiProfile profile = new RestApiProfile();
        profile.setUrl(DEFAULT_URL);
        profile.setHttpMethod(anyHttpMethod());
        profile.setTimeoutSeconds(DEFAULT_TIMEOUT_SECONDS);
        profile.setHeaders(DEFAULT_HEADERS_JSON);
        profile.setRequestBody(DEFAULT_REQUEST_BODY);
        profile.setExpectedHttpCode(DEFAULT_EXPECTED_HTTP_CODE);
        return profile;
    }

    /**
     * Полностью настраиваемая фабрика {@link RestApiProfile}.
     */
    public static RestApiProfile restProfile(
            String url,
            HttpMethod httpMethod,
            int timeoutSeconds,
            String headersJson,
            String requestBody,
            int expectedHttpCode
    ) {
        final RestApiProfile profile = new RestApiProfile();
        profile.setUrl(url);
        profile.setHttpMethod(httpMethod);
        profile.setTimeoutSeconds(timeoutSeconds);
        profile.setHeaders(headersJson);
        profile.setRequestBody(requestBody);
        profile.setExpectedHttpCode(expectedHttpCode);
        return profile;
    }

    // ===== Комбинированные удобные фабрики =====

    /**
     * Создаёт {@link Check} и прикрепляет к нему профиль по умолчанию указанного типа.
     * Удобно для тестов, где важна двунаправленная связь Check ↔ Profile.
     */
    public static Check newCheckWithProfile(String name, CheckType type) {
        final Check check = newCheck(name);
        final RestApiProfile profile = newRestProfile();
        check.attachProfile(profile, type);
        return check;
    }
}
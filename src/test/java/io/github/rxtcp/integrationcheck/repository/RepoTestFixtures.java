package io.github.rxtcp.integrationcheck.repository;

import io.github.rxtcp.integrationcheck.entity.Check;
import io.github.rxtcp.integrationcheck.entity.RestApiProfile;
import io.github.rxtcp.integrationcheck.domain.CheckType;
import io.github.rxtcp.integrationcheck.domain.HttpMethod;

import java.time.LocalDateTime;

/**
 * Тестовые фикстуры для репозиторных тестов.
 * <p>
 * Принципы:
 * - Значения по умолчанию вынесены в константы — меньше «магии» в тестах.
 * - Добавлены безопасные перегрузки/алиасы для удобства и читаемости.
 */
final class RepoTestFixtures {

    static final String DEFAULT_CHECK_DESCRIPTION = "desc";

    // ===== Значения по умолчанию ===============================================================
    static final int DEFAULT_RUN_INTERVAL_MIN = 5;
    static final String DEFAULT_URL = "https://example.org/health";
    static final HttpMethod DEFAULT_HTTP_METHOD = HttpMethod.GET;
    static final int DEFAULT_TIMEOUT_SECONDS = 30;   // [1..600]
    static final String DEFAULT_HEADERS_JSON = "{\"X-Trace-Id\":[\"abc\"]}";
    static final String DEFAULT_REQUEST_BODY = null;
    static final int DEFAULT_EXPECTED_HTTP_CODE = 200; // [100..599]
    private RepoTestFixtures() {
        // utility class
    }

    // ===== «Любые» значения enum (не завязываемся на конкретную константу) ====================

    static CheckType anyCheckType() {
        return CheckType.values()[0];
    }

    /**
     * Алиас для читаемости; исходный метод {@code anyHttp()} сохранён.
     */
    static HttpMethod anyHttpMethod() {
        return HttpMethod.values()[0];
    }

    static HttpMethod anyHttp() {
        return anyHttpMethod();
    }

    // ===== Фабрики Check =======================================================================

    /**
     * Базовая фабрика: создаёт валидный {@link Check} c указанными параметрами.
     * Сигнатура сохранена — не ломает существующие тесты.
     */
    static Check newCheck(String name, boolean enabled, LocalDateTime nextRunAt) {
        return Check.builder()
                .name(name)
                .description(DEFAULT_CHECK_DESCRIPTION)
                .enabled(enabled)
                .runIntervalMin(DEFAULT_RUN_INTERVAL_MIN)
                .nextRunAt(nextRunAt)
                .type(anyCheckType())
                .build();
    }

    // Удобная перегрузка: «включённая» проверка с default-окном запуска.
    static Check newCheck(String name) {
        return newCheck(name, true, LocalDateTime.now().plusMinutes(DEFAULT_RUN_INTERVAL_MIN));
    }

    // ===== Фабрики RestApiProfile ===============================================================

    /**
     * Базовая фабрика: создаёт валидный {@link RestApiProfile} без владельца.
     * Сигнатура сохранена — не ломает существующие тесты.
     */
    static RestApiProfile newRestProfile() {
        final RestApiProfile p = new RestApiProfile();
        p.setUrl(DEFAULT_URL);
        p.setHttpMethod(DEFAULT_HTTP_METHOD);
        p.setTimeoutSeconds(DEFAULT_TIMEOUT_SECONDS);
        p.setHeaders(DEFAULT_HEADERS_JSON);
        p.setRequestBody(DEFAULT_REQUEST_BODY);
        p.setExpectedHttpCode(DEFAULT_EXPECTED_HTTP_CODE);
        return p;
    }

    /**
     * Удобная перегрузка: создаёт валидный профиль и проставляет владельца.
     */
    static RestApiProfile newRestProfile(Check owner) {
        final RestApiProfile p = newRestProfile();
        p.setCheck(owner);
        return p;
    }

    /**
     * Алиас на случай, если где-то в тестах используется другое «историческое» имя фабрики.
     * Возвращает тот же валидный профиль, что и {@link #newRestProfile()}.
     */
    static RestApiProfile newRestApiProfile() {
        return newRestProfile();
    }
}
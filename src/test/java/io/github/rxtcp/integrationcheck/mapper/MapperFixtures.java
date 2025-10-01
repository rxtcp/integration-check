package io.github.rxtcp.integrationcheck.mapper;

import io.github.rxtcp.integrationcheck.entity.Check;
import io.github.rxtcp.integrationcheck.entity.RestApiProfile;
import io.github.rxtcp.integrationcheck.domain.CheckType;
import io.github.rxtcp.integrationcheck.domain.HttpMethod;

import java.time.LocalDateTime;

/**
 * Тестовые фикстуры для мапперов.
 * <p>
 * Подход:
 * - Значения по умолчанию вынесены в константы (меньше «магии» в тестах).
 * - Время детерминировано — стабильность на CI.
 */
final class MapperFixtures {

    // ===== Константы по умолчанию (говорящие имена) =====
    static final String DEFAULT_CHECK_DESCRIPTION = "desc";
    static final boolean DEFAULT_CHECK_ENABLED = true;
    static final int DEFAULT_RUN_INTERVAL_MIN = 5;
    // В тестах уже встречается именно этот момент времени — фиксируем как константу.
    static final LocalDateTime DEFAULT_NEXT_RUN_AT = LocalDateTime.of(2024, 1, 2, 3, 4, 5);
    static final String DEFAULT_URL = "https://example.org/health";
    static final int DEFAULT_TIMEOUT_SECONDS = 30;
    static final String DEFAULT_HEADERS_JSON = "{\"X-Trace-Id\":[\"abc\"]}";
    static final String DEFAULT_REQUEST_BODY = null;
    static final int DEFAULT_EXPECTED_HTTP_CODE = 200;
    private MapperFixtures() {
        // утилитарный класс
    }

    // ===== Утилиты выбора «любого» значения enum =====

    /**
     * Любой валидный тип проверки (не привязываемся к конкретной константе).
     */
    static CheckType anyCheckType() {
        return CheckType.values()[0];
    }

    /**
     * Любой валидный HTTP-метод (алиас для лучшей читаемости).
     * Сохранён исходный {@link #anyHttp()} для обратной совместимости.
     */
    static HttpMethod anyHttpMethod() {
        return HttpMethod.values()[0];
    }

    /**
     * Сохранено для обратной совместимости со старыми тестами.
     */
    static HttpMethod anyHttp() {
        return anyHttpMethod();
    }

    // ===== Фабрики Check =====

    /**
     * Исходная фабрика: создаёт валидный {@link Check} и проставляет id.
     * Оставлена исходная сигнатура (Long, String), чтобы не ломать тесты.
     */
    static Check newCheck(Long id, String name) {
        final Check check = Check.builder()
                .name(name)
                .description(DEFAULT_CHECK_DESCRIPTION)
                .enabled(DEFAULT_CHECK_ENABLED)
                .runIntervalMin(DEFAULT_RUN_INTERVAL_MIN)
                .nextRunAt(DEFAULT_NEXT_RUN_AT)
                .type(anyCheckType())
                .build();
        check.setId(id);
        return check;
    }

    // Удобная перегрузка (не используется существующими тестами, но пригодится):
    static Check newCheck(String name) {
        return newCheck(null, name);
    }

    // ===== Фабрики RestApiProfile =====

    /**
     * Исходная фабрика: создаёт валидный {@link RestApiProfile}, проставляет id и владельца.
     * Сохранена исходная сигнатура (Long, Check).
     */
    static RestApiProfile newRestProfile(Long id, Check owner) {
        final RestApiProfile profile = new RestApiProfile();
        profile.setId(id);
        profile.setUrl(DEFAULT_URL);
        profile.setHttpMethod(anyHttpMethod());
        profile.setTimeoutSeconds(DEFAULT_TIMEOUT_SECONDS);
        profile.setHeaders(DEFAULT_HEADERS_JSON);
        profile.setRequestBody(DEFAULT_REQUEST_BODY);
        profile.setExpectedHttpCode(DEFAULT_EXPECTED_HTTP_CODE);
        profile.setCheck(owner);
        return profile;
    }

    // Удобная перегрузка для лаконичных тестов:
    static RestApiProfile newRestProfile(Check owner) {
        return newRestProfile(null, owner);
    }
}
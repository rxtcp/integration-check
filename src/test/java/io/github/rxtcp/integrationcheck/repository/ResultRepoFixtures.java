package io.github.rxtcp.integrationcheck.repository;

import io.github.rxtcp.integrationcheck.entity.Check;
import io.github.rxtcp.integrationcheck.entity.CheckResult;
import io.github.rxtcp.integrationcheck.domain.CheckRunStatus;
import io.github.rxtcp.integrationcheck.domain.CheckType;

import java.time.LocalDateTime;

/**
 * Тестовые фикстуры для репозиторных тестов Check / CheckResult.
 * <p>
 * Принципы:
 * - Значения по умолчанию вынесены в константы (меньше «магии» в тестах).
 * - Время фиксировано (детерминированно) — стабильнее на CI.
 */
final class ResultRepoFixtures {

    static final String DEFAULT_CHECK_DESCRIPTION = "desc";

    // ===== Значения по умолчанию ===============================================================
    static final boolean DEFAULT_CHECK_ENABLED = true;
    static final int DEFAULT_RUN_INTERVAL_MIN = 5;
    // Детерминированное время: удобно в отчётах и исключает флапы
    static final LocalDateTime DEFAULT_NEXT_RUN_AT = LocalDateTime.of(2024, 1, 2, 3, 4, 5);
    static final LocalDateTime DEFAULT_STARTED_AT = LocalDateTime.of(2024, 1, 2, 3, 10, 0);
    static final LocalDateTime DEFAULT_FINISHED_AT = DEFAULT_STARTED_AT.plusSeconds(5);
    private ResultRepoFixtures() {
        // utility class
    }

    // ===== «Любые» значения enum (не завязываемся на конкретной константе) ====================

    static CheckType anyCheckType() {
        return CheckType.values()[0];
    }

    static CheckRunStatus anyStatus() {
        return CheckRunStatus.values()[0];
    }

    // ===== Фабрики Check =======================================================================

    /**
     * Базовая фабрика: создаёт валидный Check c предсказуемыми значениями.
     * Сигнатура сохранена — не ломает существующие тесты.
     */
    static Check newCheck(String name) {
        return Check.builder()
                .name(name)
                .description(DEFAULT_CHECK_DESCRIPTION)
                .enabled(DEFAULT_CHECK_ENABLED)
                .runIntervalMin(DEFAULT_RUN_INTERVAL_MIN)
                .nextRunAt(DEFAULT_NEXT_RUN_AT)
                .type(anyCheckType())
                .build();
    }

    // Удобная перегрузка: тот же Check, но с кастомным временем nextRunAt.
    static Check newCheck(String name, LocalDateTime nextRunAt) {
        return Check.builder()
                .name(name)
                .description(DEFAULT_CHECK_DESCRIPTION)
                .enabled(DEFAULT_CHECK_ENABLED)
                .runIntervalMin(DEFAULT_RUN_INTERVAL_MIN)
                .nextRunAt(nextRunAt)
                .type(anyCheckType())
                .build();
    }

    // ===== Фабрики CheckResult =================================================================

    /**
     * Базовая фабрика: валидный CheckResult без владельца (по доменному контракту это допустимо).
     * Сигнатура сохранена — не ломает существующие тесты.
     */
    static CheckResult newResult() {
        return CheckResult.builder()
                .check(null) // история без владельца — допустимо
                .startedAt(DEFAULT_STARTED_AT)
                .finishedAt(DEFAULT_FINISHED_AT) // nullable в БД, но по умолчанию задано
                .status(anyStatus())
                .failureReason(null)
                .details("ok")
                .build();
    }

    // Удобная перегрузка: результат, привязанный к конкретному Check.
    static CheckResult newResult(Check owner) {
        final CheckResult r = newResult();
        r.setCheck(owner);
        return r;
    }
}
package io.github.rxtcp.integrationcheck.entity;

import io.github.rxtcp.integrationcheck.domain.CheckRunStatus;
import io.github.rxtcp.integrationcheck.domain.CheckType;
import io.github.rxtcp.integrationcheck.domain.FailureReason;

import java.time.LocalDateTime;

/**
 * Тестовые фикстуры для результатов выполнения чеков.
 * <p>
 * Подход:
 * - Детерминированные значения времени исключают флапающие проверки (см. {@link #DEFAULT_STARTED_AT}).
 * - Добавлены перегруженные фабрики для локальной кастомизации значений в тестах.
 * - Создание {@link Check} делегировано в {@link EntityTestFixtures}, чтобы не дублировать логику.
 */
public final class ResultEntityFixtures {

    // ===== Значения по умолчанию (говорящие имена) =====
    public static final LocalDateTime DEFAULT_STARTED_AT = LocalDateTime.of(2030, 1, 1, 0, 0, 0);
    public static final LocalDateTime DEFAULT_FINISHED_AT = DEFAULT_STARTED_AT.plusSeconds(5);
    public static final String DEFAULT_DETAILS = "ok";
    private ResultEntityFixtures() {
        // утилитарный класс
    }

    // ===== Утилиты выбора «любого» значения enum (не завязываемся на конкретное имя константы) =====
    public static CheckRunStatus anyStatus() {
        return CheckRunStatus.values()[0];
    }

    public static FailureReason anyFailure() {
        return FailureReason.values()[0];
    }

    public static CheckType anyCheckType() {
        return CheckType.values()[0];
    }

    // ===== Фабрики CheckResult =====

    /**
     * Исходная фабрика (сохранена): валидный {@link CheckResult} без привязки к {@link Check}.
     * Использует детерминированные значения времени и детали {@link #DEFAULT_DETAILS}.
     */
    public static CheckResult newResult() {
        return CheckResult.builder()
                .check(null) // история без привязки
                .startedAt(DEFAULT_STARTED_AT)
                .finishedAt(DEFAULT_FINISHED_AT)
                .status(anyStatus())
                .failureReason(null)
                .details(DEFAULT_DETAILS)
                .build();
    }

    /**
     * Удобная перегрузка: валидный {@link CheckResult} с привязкой к переданному {@code check}.
     */
    public static CheckResult newResult(Check check) {
        return result(check, DEFAULT_STARTED_AT, DEFAULT_FINISHED_AT, anyStatus(), null, DEFAULT_DETAILS);
    }

    /**
     * Удобная перегрузка: настраиваем статус/причину/детали, остальное — по умолчанию.
     */
    public static CheckResult newResult(CheckRunStatus status, FailureReason failure, String details) {
        return result(null, DEFAULT_STARTED_AT, DEFAULT_FINISHED_AT, status, failure, details);
    }

    /**
     * Полностью настраиваемая фабрика {@link CheckResult}.
     */
    public static CheckResult result(
            Check check,
            LocalDateTime startedAt,
            LocalDateTime finishedAt,
            CheckRunStatus status,
            FailureReason failureReason,
            String details
    ) {
        return CheckResult.builder()
                .check(check)
                .startedAt(startedAt)
                .finishedAt(finishedAt)
                .status(status)
                .failureReason(failureReason)
                .details(details)
                .build();
    }

    // ===== Фабрики Check =====

    /**
     * Исходная фабрика (сохранена по имени и сигнатуре): создаёт валидный {@link Check} с указанным названием.
     * Делегирует в {@link EntityTestFixtures#newCheck(String)} для единообразия.
     */
    public static Check newCheck(String name) {
        return EntityTestFixtures.newCheck(name);
    }
}
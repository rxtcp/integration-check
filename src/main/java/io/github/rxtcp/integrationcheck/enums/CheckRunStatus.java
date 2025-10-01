package io.github.rxtcp.integrationcheck.enums;

/**
 * Статус выполнения проверки.
 */
public enum CheckRunStatus {
    /**
     * Выполняется.
     */
    PROCESSING,
    /**
     * Успешно завершена.
     */
    SUCCEEDED,
    /**
     * Завершена с ошибкой.
     */
    FAILED
}

package io.github.nety.integrationcheck.enums;

/**
 * Причина неуспеха выполнения проверки.
 */
public enum FailureReason {
    /**
     * Превышен таймаут.
     */
    TIMEOUT,
    /**
     * Ошибка выполнения.
     */
    ERROR,
    /**
     * Несоответствие HTTP-кода ожиданиям.
     */
    HTTP_STATUS_MISMATCH
}

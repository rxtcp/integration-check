package io.github.nety.integrationcheck.service;

/**
 * Сервис проверки состояния интеграций.
 */
public interface IntegrationHealthChecker {

    /**
     * Запустить проверки состояния интеграций.
     */
    void checkHealth();
}

package io.github.rxtcp.integrationcheck.service;

/**
 * Сервис проверки состояния интеграций.
 */
public interface IntegrationHealthChecker {

    /**
     * Запустить проверки состояния интеграций.
     */
    void checkHealth();
}

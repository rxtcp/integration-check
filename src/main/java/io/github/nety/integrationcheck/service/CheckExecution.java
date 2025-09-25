package io.github.nety.integrationcheck.service;

/**
 * Сервис запуска выполнения проверки.
 */
public interface CheckExecution {

    /**
     * Выполнить проверку по идентификатору.
     *
     * @param checkId идентификатор проверки
     */
    void execute(long checkId);
}

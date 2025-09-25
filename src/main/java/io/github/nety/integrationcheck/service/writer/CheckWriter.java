package io.github.nety.integrationcheck.service.writer;

import io.github.nety.integrationcheck.entity.Check;
import io.github.nety.integrationcheck.entity.CheckResult;

/**
 * Запись изменений сущности {@link Check}.
 */
public interface CheckWriter {

    /**
     * Обновить время следующего запуска на основании результата.
     *
     * @param check       проверка
     * @param checkResult результат текущего запуска
     * @return обновлённый {@link Check}
     */
    Check updateNextExecutionTime(Check check, CheckResult checkResult);
}

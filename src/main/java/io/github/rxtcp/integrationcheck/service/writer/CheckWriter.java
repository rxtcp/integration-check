package io.github.rxtcp.integrationcheck.service.writer;

import io.github.rxtcp.integrationcheck.entity.Check;
import io.github.rxtcp.integrationcheck.entity.CheckResult;

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

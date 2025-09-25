package io.github.nety.integrationcheck.service.writer;

import io.github.nety.integrationcheck.dto.CheckResultDto;
import io.github.nety.integrationcheck.entity.Check;
import io.github.nety.integrationcheck.entity.CheckResult;

/**
 * Запись хода и результата выполнения проверки.
 */
public interface CheckResultWriter {

    /**
     * Зафиксировать старт обработки.
     *
     * @param check проверка
     * @return созданная/сохранённая запись {@link CheckResult}
     */
    CheckResult recordProcessStart(Check check);

    /**
     * Зафиксировать завершение обработки.
     *
     * @param entity текущая запись результата
     * @param dto    итог выполнения
     * @return обновлённая запись {@link CheckResult}
     */
    CheckResult recordProcessEnd(CheckResult entity, CheckResultDto dto);
}

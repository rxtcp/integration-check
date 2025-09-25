package io.github.nety.integrationcheck.service.processor;

import io.github.nety.integrationcheck.dto.CheckDto;
import io.github.nety.integrationcheck.dto.CheckResultDto;

/**
 * Процессор выполнения проверки.
 */
public interface CheckProcessor {

    /**
     * Выполнить проверку.
     *
     * @param check входные данные
     * @return результат
     */
    CheckResultDto process(CheckDto check);
}

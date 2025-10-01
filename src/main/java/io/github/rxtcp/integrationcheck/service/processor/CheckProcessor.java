package io.github.rxtcp.integrationcheck.service.processor;

import io.github.rxtcp.integrationcheck.dto.CheckDto;
import io.github.rxtcp.integrationcheck.dto.CheckResultDto;

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

package io.github.nety.integrationcheck.service.processor.strategy;

import io.github.nety.integrationcheck.dto.CheckDto;
import io.github.nety.integrationcheck.dto.CheckResultDto;
import io.github.nety.integrationcheck.enums.CheckType;

/**
 * Стратегия выполнения проверки (Strategy) для конкретного {@link CheckType}.
 */
public interface CheckStrategy {

    /**
     * Поддерживаемый тип проверки.
     */
    CheckType getType();

    /**
     * Выполнить проверку.
     *
     * @param check входные данные проверки
     * @return результат выполнения
     */
    CheckResultDto execute(CheckDto check);
}

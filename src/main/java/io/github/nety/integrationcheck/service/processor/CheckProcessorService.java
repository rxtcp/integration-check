package io.github.nety.integrationcheck.service.processor;

import io.github.nety.integrationcheck.dto.CheckDto;
import io.github.nety.integrationcheck.dto.CheckResultDto;
import io.github.nety.integrationcheck.service.processor.strategy.CheckStrategyFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED;

/**
 * Делегирует выполнение проверки в стратегию по типу. Выполняется без транзакции.
 */
@RequiredArgsConstructor
@Service
public class CheckProcessorService implements CheckProcessor {

    /** Реестр стратегий. */
    private final CheckStrategyFactory strategyFactory;

    /**
     * Выполнить проверку.
     * @param check входные данные
     * @return результат
     */
    @Transactional(propagation = NOT_SUPPORTED)
    @Override
    public CheckResultDto process(CheckDto check) {
        return strategyFactory.getStrategy(check.type())
                .execute(check);
    }
}

package io.github.rxtcp.integrationcheck.service;

import io.github.rxtcp.integrationcheck.dto.CheckDto;
import io.github.rxtcp.integrationcheck.dto.CheckResultDto;
import io.github.rxtcp.integrationcheck.entity.Check;
import io.github.rxtcp.integrationcheck.entity.CheckResult;
import io.github.rxtcp.integrationcheck.mapper.CheckMapper;
import io.github.rxtcp.integrationcheck.service.processor.CheckProcessor;
import io.github.rxtcp.integrationcheck.service.reader.CheckReader;
import io.github.rxtcp.integrationcheck.service.writer.CheckResultWriter;
import io.github.rxtcp.integrationcheck.service.writer.CheckWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static io.github.rxtcp.integrationcheck.enums.CheckRunStatus.FAILED;
import static io.github.rxtcp.integrationcheck.enums.FailureReason.ERROR;

/**
 * Оркестрация выполнения проверки: чтение, запуск, фиксация результата, обновление расписания.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class CheckExecutionService implements CheckExecution {

    private final CheckMapper checkMapper;
    private final CheckReader checkReader;
    private final CheckProcessor checkProcessor;
    private final CheckWriter checkWriter;
    private final CheckResultWriter checkResultWriter;

    /**
     * Запуск проверки по идентификатору.
     * Выполняется вне транзакции (NOT_SUPPORTED); внутренние операции транзакционны.
     *
     * @param checkId идентификатор проверки
     */
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void execute(long checkId) {
        try {
            var checkEntity = checkReader.findWithProfileById(checkId);
            var checkResultEntity = recordStart(checkEntity);
            var resultDto = processCheckSafely(checkEntity);
            finalizeCheckExecution(checkEntity, checkResultEntity, resultDto);
        } catch (Exception exception) {
            log.error("Ошибка при выполнении проверки id={}: {}", checkId, exception.getMessage(), exception);
        }
    }

    /**
     * Обёртка над процессингом: перехватывает ошибки и возвращает FAILED/ERROR.
     */
    private CheckResultDto processCheckSafely(Check checkEntity) {
        try {
            return processCheck(checkEntity);
        } catch (Exception exception) {
            log.error("Ошибка во время проверки: {}, message={}", checkEntity, exception.getMessage(), exception);
            return new CheckResultDto(FAILED, ERROR, exception.getMessage());
        }
    }

    /**
     * Фиксирует начало выполнения и логирует.
     */
    private CheckResult recordStart(Check checkEntity) {
        var startedCheckResultEntity = checkResultWriter.recordProcessStart(checkEntity);
        log.info("Начало проверки: {}, {}", checkEntity, startedCheckResultEntity);
        return startedCheckResultEntity;
    }

    /**
     * Маппит сущность в DTO и делегирует выполнение процессору.
     */
    private CheckResultDto processCheck(Check checkEntity) {
        CheckDto checkDto = checkMapper.toDto(checkEntity);
        return checkProcessor.process(checkDto);
    }

    /**
     * Фиксирует завершение, обновляет nextRunAt и логирует.
     */
    private void finalizeCheckExecution(Check checkEntity, CheckResult checkResultEntity, CheckResultDto resultDto) {
        var updatedCheckResultEntity = checkResultWriter.recordProcessEnd(checkResultEntity, resultDto);
        var updatedCheckEntity = checkWriter.updateNextExecutionTime(checkEntity, updatedCheckResultEntity);
        log.info("Конец проверки: {}, {}", updatedCheckEntity, updatedCheckResultEntity);
    }
}
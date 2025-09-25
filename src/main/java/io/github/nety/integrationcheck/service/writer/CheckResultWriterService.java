package io.github.nety.integrationcheck.service.writer;

import io.github.nety.integrationcheck.dto.CheckResultDto;
import io.github.nety.integrationcheck.entity.Check;
import io.github.nety.integrationcheck.entity.CheckResult;
import io.github.nety.integrationcheck.repository.CheckResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static io.github.nety.integrationcheck.enums.CheckRunStatus.PROCESSING;

/**
 * Сервис фиксации начала/завершения выполнения проверки. Все методы — в транзакции.
 */
@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class CheckResultWriterService implements CheckResultWriter {

    /**
     * Репозиторий для сохранения результатов.
     */
    private final CheckResultRepository checkResultRepository;

    /**
     * Создать запись о старте обработки.
     * Устанавливает {@code startedAt=now()}, статус {@code PROCESSING}.
     *
     * @param check проверка
     * @return сохранённый {@link CheckResult}
     */
    @Override
    public CheckResult recordProcessStart(Check check) {
        var checkResult = CheckResult.builder()
                .check(check)
                .startedAt(LocalDateTime.now())
                .status(PROCESSING)
                .build();
        return checkResultRepository.save(checkResult);
    }

    /**
     * Зафиксировать завершение обработки.
     * Обновляет {@code finishedAt=now()}, статус/причину/детали из {@code dto}.
     *
     * @param entity текущая запись
     * @param dto    итог выполнения
     * @return сохранённый {@link CheckResult}
     */
    @Override
    public CheckResult recordProcessEnd(CheckResult entity, CheckResultDto dto) {
        entity.setFinishedAt(LocalDateTime.now());
        entity.setStatus(dto.status());
        entity.setFailureReason(dto.failureReason());
        entity.setDetails(dto.details());
        return checkResultRepository.save(entity);
    }
}

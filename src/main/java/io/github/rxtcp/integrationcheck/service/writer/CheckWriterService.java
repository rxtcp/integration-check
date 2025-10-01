package io.github.rxtcp.integrationcheck.service.writer;

import io.github.rxtcp.integrationcheck.entity.Check;
import io.github.rxtcp.integrationcheck.entity.CheckResult;
import io.github.rxtcp.integrationcheck.repository.CheckRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.transaction.annotation.Isolation.READ_COMMITTED;
import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

/**
 * Сервис обновления планового времени запуска проверки.
 */
@RequiredArgsConstructor
@Transactional(isolation = READ_COMMITTED, propagation = REQUIRES_NEW)
@Service
public class CheckWriterService implements CheckWriter {

    /**
     * Репозиторий {@link Check}.
     */
    private final CheckRepository checkRepository;

    /**
     * Обновляет {@code nextRunAt = finishedAt + runIntervalMin} и сохраняет {@link Check}.
     *
     * @param check       проверка
     * @param checkResult результат текущего запуска (используется {@code finishedAt})
     * @return обновлённая сущность {@link Check}
     */
    @Override
    public Check updateNextExecutionTime(Check check, CheckResult checkResult) {
        var finishedAt = checkResult.getFinishedAt();
        var nextRunAt = finishedAt.plusMinutes(check.getRunIntervalMin());
        check.setNextRunAt(nextRunAt);
        return checkRepository.save(check);
    }
}

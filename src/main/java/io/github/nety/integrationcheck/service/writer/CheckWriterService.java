package io.github.nety.integrationcheck.service.writer;

import io.github.nety.integrationcheck.entity.Check;
import io.github.nety.integrationcheck.entity.CheckResult;
import io.github.nety.integrationcheck.repository.CheckRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Сервис обновления планового времени запуска проверки.
 */
@RequiredArgsConstructor
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
    @Transactional
    @Override
    public Check updateNextExecutionTime(Check check, CheckResult checkResult) {
        var finishedAt = checkResult.getFinishedAt();
        var nextRunAt = finishedAt.plusMinutes(check.getRunIntervalMin());
        check.setNextRunAt(nextRunAt);
        return checkRepository.save(check);
    }
}

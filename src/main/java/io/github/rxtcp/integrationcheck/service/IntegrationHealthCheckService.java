package io.github.rxtcp.integrationcheck.service;

import io.github.rxtcp.integrationcheck.configuration.properties.IntegrationHealthCheckJobProps;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;

/**
 * Сервис запуска Spring Batch job для проверки интеграций.
 * <p>
 * Формирует идемпотентные {@link JobParameters} с временным окном и запускает job асинхронно.
 * Параметр {@code windowStart} используется как идентифицирующий — это предотвращает повторный
 * запуск одного и того же «окна» времени.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class IntegrationHealthCheckService implements IntegrationHealthChecker {

    /**
     * Имя идентифицирующего параметра: начало окна (UTC, millis since epoch).
     */
    public static final String PARAM_WINDOW_START = "windowStart";

    private final JobLauncher asyncJobLauncher;
    private final Job integrationHealthCheckJob;
    private final IntegrationHealthCheckJobProps.Schedule properties;

    /**
     * Вычисляет начало окна в миллисекундах от эпохи на границе, кратной {@code windowDuration}.
     * Пример: при окне 60s и времени 12:03:47 → 12:03:00.
     *
     * @param clock          источник времени (обычно {@link Clock#systemUTC()})
     * @param windowDuration длительность окна, должна быть &gt; 0
     * @return миллисекунды начала окна (UTC)
     */
    private static long computeWindowStartEpochMillis(Clock clock, Duration windowDuration) {
        long windowMillis = windowDuration.toMillis();
        long nowMillis = clock.millis();
        return Math.floorDiv(nowMillis, windowMillis) * windowMillis;
    }

    /**
     * Запускает проверку интеграций с рассчитанными параметрами окна.
     * <p>
     * Метод безопасен к повторным вызовам: при параллельном запуске или уже выполненном окне
     * логирует предупреждение и завершает выполнение без исключений наружу.
     */
    @Override
    public void checkHealth() {
        var jobParameters = buildJobParameters();
        try {
            asyncJobLauncher.run(integrationHealthCheckJob, jobParameters);
            log.info("Запущена проверка интеграций.");
        } catch (JobExecutionAlreadyRunningException e) {
            log.info("Параллельный экземпляр уже запустил проверку интеграций.");
        } catch (JobInstanceAlreadyCompleteException | JobRestartException e) {
            log.info("Проверка интеграций уже выполнена.");
        } catch (JobParametersInvalidException e) {
            log.error("Неверные JobParameters: {}", jobParameters);
        }
    }

    /**
     * Строит параметры job с идентифицирующим параметром {@link #PARAM_WINDOW_START}.
     * Значение — начало текущего окна на границе кратной {@code windowSeconds}.
     */
    private JobParameters buildJobParameters() {
        var windowStart = computeWindowStartEpochMillis(Clock.systemUTC(), Duration.ofSeconds(properties.windowSeconds()));
        return new JobParametersBuilder()
                .addLong(PARAM_WINDOW_START, windowStart, true) // identifying = true
                .toJobParameters();
    }
}
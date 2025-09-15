package io.github.nety.integrationcheck.service;

import io.github.nety.integrationcheck.config.properties.IntegrationHealthCheckJobProperties;
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
import java.time.Instant;

@Slf4j
@RequiredArgsConstructor
@Service
public class IntegrationHealthCheckService implements IntegrationHealthChecker {

    public static final String PARAM_WINDOW_START = "windowStart";

    private final JobLauncher asyncJobLauncher;
    private final Job integrationHealthCheckJob;
    private final IntegrationHealthCheckJobProperties.Schedule properties;

    @Override
    public void checkHealth() {
        var jobParameters = buildJobParameters();
        try {
            asyncJobLauncher.run(integrationHealthCheckJob, jobParameters);
            log.warn("Запущена проверка интеграций.");
        } catch (JobExecutionAlreadyRunningException e) {
            log.warn("Параллельный экземпляр уже запустил проверку интеграций.");
        } catch (JobInstanceAlreadyCompleteException | JobRestartException e) {
            log.warn("Проверка интеграций уже выполнена.");
        } catch (JobParametersInvalidException e) {
            log.error("Неверные JobParameters: {}", jobParameters);
        }
    }

    private JobParameters buildJobParameters() {
        var windowStart = computeWindowStartEpochMillis(Clock.systemUTC(), Duration.ofSeconds(properties.windowSeconds()));
        return new JobParametersBuilder()
                .addLong(PARAM_WINDOW_START, windowStart, true)
                .toJobParameters();
    }

    private static long computeWindowStartEpochMillis(Clock clock, Duration windowDuration) {
        long windowMillis = windowDuration.toMillis();
        long nowMillis = clock.millis();
        return Math.floorDiv(nowMillis, windowMillis) * windowMillis;
    }
}

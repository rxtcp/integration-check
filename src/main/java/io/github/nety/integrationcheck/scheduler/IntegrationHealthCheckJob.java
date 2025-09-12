package io.github.nety.integrationcheck.scheduler;

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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;


@Slf4j
@Service
@RequiredArgsConstructor
public class IntegrationHealthCheckJob {

    private static final Duration WINDOW = Duration.ofSeconds(30);

    private final JobLauncher jobLauncher;
    private final Job integrationHealthJob;

    private final Clock clock = Clock.systemUTC();

    @Scheduled(cron = "0/30 * * * * *", zone = "${app.scheduler.zone:UTC}")
    public void tick() {
        Instant now = Instant.now(clock);
        Instant start = createWindowStart(now);
        Instant end = createWindowEnd(start);

        JobParameters params = buildJobParameters(start, end);

        try {
            jobLauncher.run(integrationHealthJob, params);
            log.warn("Запущена проверка интеграций для окна [{}..{}).", start, end);
        } catch (JobExecutionAlreadyRunningException e) {
            log.warn("Параллельный экземпляр уже запустил проверку интеграций [{}..{}).", start, end);
        } catch (JobInstanceAlreadyCompleteException | JobRestartException e) {
            log.warn("Окно [{}..{}) уже обработано ({}).", start, end, e.getClass().getSimpleName());
        } catch (JobParametersInvalidException e) {
            log.error("Неверные JobParameters: {}", params, e);
        }
    }

    /**
     * Создаёт параметры джоба из границ окна
     */
    private JobParameters buildJobParameters(Instant windowStart, Instant windowEnd) {
        return new JobParametersBuilder()
                .addLong("windowStart", windowStart.toEpochMilli(), true)
                .addLong("windowEnd", windowEnd.toEpochMilli())
                .toJobParameters();
    }

    /**
     * Отдаёт начало окна, выровненное вниз по длительности окна относительно произвольного времени
     */
    private Instant createWindowStart(Instant referenceTime) {
        long w = WINDOW.toMillis();
        long t = referenceTime.toEpochMilli();
        return Instant.ofEpochMilli(t - Math.floorMod(t, w));
    }

    /**
     * Отдаёт конец окна на основе известного начала
     */
    private Instant createWindowEnd(Instant windowStart) {
        return windowStart.plus(WINDOW);
    }
}

package io.github.nety.integrationcheck.scheduler;

import io.github.nety.integrationcheck.service.IntegrationHealthChecker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Планировщик периодической проверки состояния внешних интеграций.
 * <p>
 * Включается только при {@code app.spring-batch.jobs.integration-health-check-job.schedule.enabled=true}.
 * Расписание и часовой пояс берутся из:
 * <ul>
 *   <li>{@code app.spring-batch.jobs.integration-health-check-job.schedule.cron}</li>
 *   <li>{@code app.spring-batch.jobs.integration-health-check-job.schedule.zone}</li>
 * </ul>
 *
 * @see IntegrationHealthChecker
 */
@ConditionalOnProperty(
        value = "app.spring-batch.jobs.integration-health-check-job.schedule.enabled",
        havingValue = "true"
)
@Slf4j
@RequiredArgsConstructor
@Service
public class IntegrationHealthCheckScheduler {

    /**
     * Сервис, выполняющий фактическую проверку здоровья интеграций.
     */
    private final IntegrationHealthChecker healthChecker;

    /**
     * Точка входа планировщика.
     * <p>
     * Выполняется по cron-расписанию из конфигурации приложения и инициирует проверку
     * всех поддерживаемых интеграций. Метод предполагается идемпотентным и быстрым,
     * чтобы не блокировать поток планировщика.
     */
    @Scheduled(
            cron = "${app.spring-batch.jobs.integration-health-check-job.schedule.cron}",
            zone = "${app.spring-batch.jobs.integration-health-check-job.schedule.zone}"
    )
    public void tick() {
        healthChecker.checkHealth();
    }
}
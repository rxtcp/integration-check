package io.github.rxtcp.integrationcheck.configuration.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Свойства job'а интеграционной проверки (aggregation-класс).
 * <p>
 * Содержит вложенные группы параметров:
 * <ul>
 *   <li>{@link WorkerStepProps} — параметры шага-воркера;</li>
 *   <li>{@link Schedule} — расписание запуска job'а.</li>
 * </ul>
 * Значения по умолчанию и источники переменных окружения описаны в {@code application.yml}.
 */
public record IntegrationHealthCheckJobProps() {

    /**
     * Свойства шага-воркера (префикс: {@code application.spring-batch.jobs.integration-health-check-job.worker-step}).
     *
     * @param concurrencyLimit      предел параллелизма шага (кол-во одновременных обработчиков), значение ≥ 1
     * @param threadNamePrefix      префикс имени потоков исполнителей шага (для удобной трассировки в логах/метриках)
     * @param virtualThreadsEnabled включить виртуальные потоки для исполнителей шага
     */
    @Validated
    @ConfigurationProperties("application.spring-batch.jobs.integration-health-check-job.worker-step")
    public record WorkerStepProps(
            @Min(1) int concurrencyLimit,
            @NotBlank String threadNamePrefix,
            boolean virtualThreadsEnabled
    ) {
    }

    /**
     * Свойства расписания запуска job'а (префикс: {@code application.spring-batch.jobs.integration-health-check-job.schedule}).
     *
     * @param enabled        включить/выключить планировщик запуска job'а
     * @param cron           CRON-выражение из 6 полей (включая секунды), например {@code 0/30 * * * * *}
     * @param zone           часовой пояс планировщика, например {@code Europe/Moscow}
     * @param windowSeconds  окно дедупликации запусков в секундах (значение ≥ 1) для защиты от наложений триггеров
     */
    @Validated
    @ConfigurationProperties("application.spring-batch.jobs.integration-health-check-job.schedule")
    public record Schedule(
            boolean enabled,
            @NotBlank String cron,
            @NotBlank String zone,
            @Min(1) int windowSeconds
    ) {
    }
}

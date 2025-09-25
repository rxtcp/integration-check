package io.github.nety.integrationcheck.configuration.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Свойства джобы Integration Health Check
 * (префикс: {@code app.spring-batch.jobs.integration-health-check-job}).
 *
 * @param concurrencyLimit      предел параллелизма (≥ 1)
 * @param threadNamePrefix      префикс имени потоков
 * @param virtualThreadsEnabled использовать виртуальные потоки
 */
@Validated
@ConfigurationProperties("app.spring-batch.jobs.integration-health-check-job")
public record IntegrationHealthCheckJobProps(
        @Min(1) int concurrencyLimit,
        @NotBlank String threadNamePrefix,
        boolean virtualThreadsEnabled
) {

    /**
     * Расписание запуска
     * (префикс: {@code app.spring-batch.jobs.integration-health-check-job.schedule}).
     *
     * @param enabled       включить плановый запуск
     * @param cron          CRON-выражение
     * @param zone          таймзона
     * @param windowSeconds окно выполнения, сек (≥ 1)
     */
    @ConfigurationProperties("app.spring-batch.jobs.integration-health-check-job.schedule")
    public record Schedule(
            boolean enabled,
            @NotBlank String cron,
            @NotBlank String zone,
            @Min(1) int windowSeconds
    ) {
    }
}

package io.github.nety.integrationcheck.config.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.spring-batch.jobs.integration-health-check-job")
public record IntegrationHealthCheckJobProperties(
        @Min(1) int chunkSize,
        @Min(1) int concurrencyLimit,
        @NotBlank String threadNamePrefix
) {

    @ConfigurationProperties("app.spring-batch.jobs.integration-health-check-job.schedule")
    public record Schedule(
            boolean enabled,
            @NotBlank String cron,
            @NotBlank String zone,
            @Min(1) int windowSeconds
    ) {
    }
}
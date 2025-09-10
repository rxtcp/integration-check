package io.github.nety.integrationcheck.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.validation.annotation.Validated;

import java.time.ZoneId;

@Validated
@ConfigurationProperties(prefix = "app.jobs.integration-health-check")
public record IntegrationHealthCheckJobProperties(
        boolean enabled,
        @NotBlank String cron,
        ZoneId zone
) {

    /**
     * Валидация cron-выражения в формате Spring (секунды обязательны).
     */
    @AssertTrue(message = "Свойство 'cron' должно быть корректным cron-выражением Spring (с обязательным указанием секунд).")
    public boolean isCronValid() {
        try {
            CronExpression.parse(cron);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}
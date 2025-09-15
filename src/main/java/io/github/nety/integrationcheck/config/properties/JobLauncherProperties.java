package io.github.nety.integrationcheck.config.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.spring-batch.job-launcher")
public record JobLauncherProperties(
        @Min(1) int concurrencyLimit,
        @NotBlank String threadNamePrefix
) {
}

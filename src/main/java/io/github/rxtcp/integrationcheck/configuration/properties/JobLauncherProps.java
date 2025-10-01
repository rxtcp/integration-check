package io.github.rxtcp.integrationcheck.configuration.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Свойства JobLauncher (префикс: {@code application.spring-batch.job-launcher}).
 *
 * @param virtualThreadsEnabled включить виртуальные потоки
 * @param concurrencyLimit      предел параллелизма (≥ 1)
 * @param threadNamePrefix      префикс имени потока
 */
@Validated
@ConfigurationProperties("application.spring-batch.job-launcher")
public record JobLauncherProps(
        boolean virtualThreadsEnabled,
        @Min(1) int concurrencyLimit,
        @NotBlank String threadNamePrefix
) {
}

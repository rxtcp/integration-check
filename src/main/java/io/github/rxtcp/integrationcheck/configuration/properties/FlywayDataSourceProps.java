package io.github.rxtcp.integrationcheck.configuration.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Свойства источника данных Flyway (префикс: {@code application.flyway.datasource}).
 * <p>
 * Используются для подключения отдельного datasource, через который выполняются миграции,
 * чтобы изолировать их от рабочего пула приложения и снизить конкуренцию за соединения.
 *
 * @param url      JDBC-URL БД, к которой применяются миграции
 *                 (пример: {@code jdbc:postgresql://host:5432/db?prepareThreshold=0})
 * @param username имя пользователя, под которым выполняются миграции
 * @param password пароль пользователя миграций
 */
@Validated
@ConfigurationProperties("application.flyway.datasource")
public record FlywayDataSourceProps(
        @NotBlank String url,
        @NotBlank String username,
        @NotNull String password
) {

    /**
     * Параметры пула HikariCP для datasource миграций Flyway
     * (префикс: {@code application.flyway.datasource.hikari}).
     * <p>
     * Как правило, пул для миграций держат небольшим, поскольку выполнение происходит преимущественно
     * на старте приложения и не требует высокой параллельности.
     *
     * @param poolName        имя пула (удобно для логов и метрик)
     * @param maximumPoolSize максимальное число соединений (≥ 1)
     * @param minimumIdle     минимальное число простаивающих соединений (≥ 0)
     */
    @Validated
    @ConfigurationProperties("application.flyway.datasource.hikari")
    public record Hikari(
            @NotBlank String poolName,
            @Min(1) int maximumPoolSize,
            @Min(0) int minimumIdle
    ) {
    }
}

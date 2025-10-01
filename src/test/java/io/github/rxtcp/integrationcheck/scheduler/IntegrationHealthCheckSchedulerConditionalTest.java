package io.github.rxtcp.integrationcheck.scheduler;

import io.github.rxtcp.integrationcheck.service.IntegrationHealthChecker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Проверяет условное создание бина {@link IntegrationHealthCheckScheduler}
 * в зависимости от флага:
 * application.spring-batch.jobs.integration-health-check-job.schedule.enabled
 */
@DisplayName("IntegrationHealthCheckScheduler: условная регистрация бина")
@DisplayNameGeneration(ReplaceUnderscores.class)
class IntegrationHealthCheckSchedulerConditionalTest {

    // Базовые ключи свойств — без «магических строк»
    private static final String PREFIX = "application.spring-batch.jobs.integration-health-check-job.schedule";
    private static final String ENABLED = PREFIX + ".enabled";
    private static final String CRON = PREFIX + ".cron";
    private static final String ZONE = PREFIX + ".zone";

    /**
     * Минимальная конфигурация контекста: сам планировщик и заглушка сервиса.
     */
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(IntegrationHealthCheckScheduler.class, TestConfig.class);

    @Nested
    @DisplayName("Когда scheduler включён")
    class When_enabled {

        @Test
        void should_create_scheduler_bean() {
            contextRunner
                    .withPropertyValues(
                            ENABLED + "=true",
                            CRON + "=0 * * * * *",
                            ZONE + "=UTC"
                    )
                    .run(ctx -> assertThat(ctx).hasSingleBean(IntegrationHealthCheckScheduler.class));
        }
    }

    @Nested
    @DisplayName("Когда scheduler выключен или свойство отсутствует")
    class When_disabled_or_missing_flag {

        @Test
        void should_not_create_scheduler_bean_when_disabled() {
            contextRunner
                    .withPropertyValues(ENABLED + "=false")
                    .run(ctx -> assertThat(ctx).doesNotHaveBean(IntegrationHealthCheckScheduler.class));
        }

        @Test
        void should_not_create_scheduler_bean_when_flag_is_missing() {
            // matchIfMissing=false по умолчанию → бин не регистрируется
            contextRunner
                    .run(ctx -> assertThat(ctx).doesNotHaveBean(IntegrationHealthCheckScheduler.class));
        }
    }

    @TestConfiguration
    static class TestConfig {
        /** Мокаем зависимость, чтобы контекст поднимался быстро и изолированно. */
        @Bean
        IntegrationHealthChecker healthChecker() {
            return mock(IntegrationHealthChecker.class);
        }
    }
}
package io.github.nety.integrationcheck.scheduler;

import io.github.nety.integrationcheck.service.IntegrationHealthChecker;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Проверяет, что бин создаётся/не создаётся в зависимости от свойства
 * app.spring-batch.jobs.integration-health-check-job.schedule.enabled.
 */
class IntegrationHealthCheckSchedulerConditionalTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(IntegrationHealthCheckScheduler.class, TestConfig.class);

    @Test
    void beanCreatedWhenEnabled() {
        contextRunner
                .withPropertyValues(
                        "app.spring-batch.jobs.integration-health-check-job.schedule.enabled=true",
                        "app.spring-batch.jobs.integration-health-check-job.schedule.cron=0 * * * * *",
                        "app.spring-batch.jobs.integration-health-check-job.schedule.zone=UTC"
                )
                .run(ctx -> assertThat(ctx).hasSingleBean(IntegrationHealthCheckScheduler.class));
    }

    @Test
    void beanNotCreatedWhenDisabled() {
        contextRunner
                .withPropertyValues(
                        "app.spring-batch.jobs.integration-health-check-job.schedule.enabled=false"
                )
                .run(ctx -> assertThat(ctx).doesNotHaveBean(IntegrationHealthCheckScheduler.class));
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
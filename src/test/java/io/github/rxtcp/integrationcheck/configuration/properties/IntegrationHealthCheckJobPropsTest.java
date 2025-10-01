package io.github.rxtcp.integrationcheck.configuration.properties;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesBindException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты биндинга и валидации свойств {@link IntegrationHealthCheckJobProps}.
 * <p>
 * Идея:
 * - валидный набор свойств должен корректно биндиться в соответствующие бины;
 * - нарушение ограничений валидации (Min/NotBlank и т.п.) приводит к ошибке биндинга.
 */
@DisplayName("IntegrationHealthCheckJobProps: биндинг и валидация")
@DisplayNameGeneration(ReplaceUnderscores.class)
class IntegrationHealthCheckJobPropsTest {

    /**
     * Общая автоконфигурация для биндинга и валидации.
     */
    private static final AutoConfigurations AUTO_CONFIGS = AutoConfigurations.of(
            ConfigurationPropertiesAutoConfiguration.class,
            ValidationAutoConfiguration.class
    );

    // --- Конфигурации, регистрирующие ровно один бин свойств ---

    @Configuration
    @EnableConfigurationProperties(IntegrationHealthCheckJobProps.WorkerStepProps.class)
    static class WorkerStepPropsConfig {
    }

    @Configuration
    @EnableConfigurationProperties(IntegrationHealthCheckJobProps.Schedule.class)
    static class ScheduleConfig {
    }

    @Nested
    @DisplayName("WorkerStepProps")
    class WorkerStepPropsTests {

        private static final String PREFIX = "application.spring-batch.jobs.integration-health-check-job.worker-step";
        private static final String CONCURRENCY_LIMIT = PREFIX + ".concurrency-limit";
        private static final String THREAD_NAME_PREFIX = PREFIX + ".thread-name-prefix";
        private static final String VIRTUAL_THREADS_ENABLED = PREFIX + ".virtual-threads-enabled";

        private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
                .withConfiguration(AUTO_CONFIGS)
                .withUserConfiguration(WorkerStepPropsConfig.class);

        /**
         * Негативные сценарии:
         * - concurrencyLimit < 1 → @Min(1);
         * - threadNamePrefix blank → @NotBlank.
         */
        static Stream<Object[]> invalidCases() {
            return Stream.of(
                    new Object[]{
                            "concurrencyLimit < 1",
                            new String[]{
                                    CONCURRENCY_LIMIT + "=0",
                                    THREAD_NAME_PREFIX + "=hc-",
                                    VIRTUAL_THREADS_ENABLED + "=false"
                            }
                    },
                    new Object[]{
                            "threadNamePrefix blank",
                            new String[]{
                                    CONCURRENCY_LIMIT + "=2",
                                    THREAD_NAME_PREFIX + "=   ",
                                    VIRTUAL_THREADS_ENABLED + "=false"
                            }
                    }
            );
        }

        @Test
        void should_bind_valid_properties() {
            contextRunner.withPropertyValues(
                    CONCURRENCY_LIMIT + "=4",
                    THREAD_NAME_PREFIX + "=hc-worker-",
                    VIRTUAL_THREADS_ENABLED + "=true"
            ).run(ctx -> {
                assertThat(ctx).hasNotFailed();
                final var props = ctx.getBean(IntegrationHealthCheckJobProps.WorkerStepProps.class);
                assertThat(props.concurrencyLimit()).isEqualTo(4);
                assertThat(props.threadNamePrefix()).isEqualTo("hc-worker-");
                assertThat(props.virtualThreadsEnabled()).isTrue();
            });
        }

        @ParameterizedTest(name = "[{index}] {0}")
        @MethodSource("invalidCases")
        void should_fail_binding_when_validation_constraints_are_violated(String caseName, String[] propertyValues) {
            contextRunner.withPropertyValues(propertyValues).run(ctx -> {
                assertThat(ctx).hasFailed();
                assertThat(ctx.getStartupFailure())
                        .isInstanceOf(ConfigurationPropertiesBindException.class)
                        .hasMessageContaining("worker-step");
            });
        }
    }

    @Nested
    @DisplayName("Schedule")
    class ScheduleTests {

        private static final String PREFIX = "application.spring-batch.jobs.integration-health-check-job.schedule";
        private static final String ENABLED = PREFIX + ".enabled";
        private static final String CRON = PREFIX + ".cron";
        private static final String ZONE = PREFIX + ".zone";
        private static final String WINDOW_SECONDS = PREFIX + ".window-seconds";

        private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
                .withConfiguration(AUTO_CONFIGS)
                .withUserConfiguration(ScheduleConfig.class);

        /**
         * Негативные сценарии:
         * - windowSeconds < 1 → @Min(1);
         * - cron blank → @NotBlank;
         * - zone blank → @NotBlank.
         */
        static Stream<Object[]> invalidCases() {
            return Stream.of(
                    new Object[]{
                            "windowSeconds < 1",
                            new String[]{
                                    ENABLED + "=true",
                                    CRON + "=0/15 * * * * *",
                                    ZONE + "=UTC",
                                    WINDOW_SECONDS + "=0"
                            }
                    },
                    new Object[]{
                            "cron blank",
                            new String[]{
                                    ENABLED + "=false",
                                    CRON + "=   ",
                                    ZONE + "=Europe/Moscow",
                                    WINDOW_SECONDS + "=5"
                            }
                    },
                    new Object[]{
                            "zone blank",
                            new String[]{
                                    ENABLED + "=false",
                                    CRON + "=0/30 * * * * *",
                                    ZONE + "=   ",
                                    WINDOW_SECONDS + "=5"
                            }
                    }
            );
        }

        @Test
        void should_bind_valid_properties() {
            contextRunner.withPropertyValues(
                    ENABLED + "=true",
                    CRON + "=0/30 * * * * *",
                    ZONE + "=Europe/Moscow",
                    WINDOW_SECONDS + "=30"
            ).run(ctx -> {
                assertThat(ctx).hasNotFailed();
                final var props = ctx.getBean(IntegrationHealthCheckJobProps.Schedule.class);
                assertThat(props.enabled()).isTrue();
                assertThat(props.cron()).isEqualTo("0/30 * * * * *");
                assertThat(props.zone()).isEqualTo("Europe/Moscow");
                assertThat(props.windowSeconds()).isEqualTo(30);
            });
        }

        @ParameterizedTest(name = "[{index}] {0}")
        @MethodSource("invalidCases")
        void should_fail_binding_when_validation_constraints_are_violated(String caseName, String[] propertyValues) {
            contextRunner.withPropertyValues(propertyValues).run(ctx -> {
                assertThat(ctx).hasFailed();
                assertThat(ctx.getStartupFailure())
                        .isInstanceOf(ConfigurationPropertiesBindException.class)
                        .hasMessageContaining("schedule");
            });
        }
    }
}
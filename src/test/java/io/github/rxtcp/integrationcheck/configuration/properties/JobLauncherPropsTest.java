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
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.validation.FieldError;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты биндинга и валидации свойств {@link JobLauncherProps}.
 * <p>
 * Цели:
 * 1) Валидный набор свойств корректно биндится в бин конфигурации.
 * 2) Нарушение ограничений валидации приводит к ошибке биндинга с ожидаемыми кодами.
 */
@DisplayName("JobLauncherProps: биндинг и валидация")
@DisplayNameGeneration(ReplaceUnderscores.class)
class JobLauncherPropsTest {

    private static final AutoConfigurations AUTO_CONFIGS = AutoConfigurations.of(
            ValidationAutoConfiguration.class,
            ConfigurationPropertiesAutoConfiguration.class
    );

    private static final String PREFIX = "application.spring-batch.job-launcher";
    private static final String VIRTUAL_THREADS_ENABLED = PREFIX + ".virtual-threads-enabled";
    private static final String CONCURRENCY_LIMIT = PREFIX + ".concurrency-limit";
    private static final String THREAD_NAME_PREFIX = PREFIX + ".thread-name-prefix";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AUTO_CONFIGS)
            .withUserConfiguration(TestConfig.class);

    /**
     * Набор негативных кейсов:
     * - concurrencyLimit < 1 → @Min(1)
     * - threadNamePrefix пробельный → @NotBlank
     */
    static Stream<Object[]> invalidCases() {
        return Stream.of(
                new Object[]{
                        "concurrencyLimit < 1 → ошибка @Min(1)",
                        new String[]{
                                VIRTUAL_THREADS_ENABLED + "=true",
                                CONCURRENCY_LIMIT + "=0",
                                THREAD_NAME_PREFIX + "=x-"
                        },
                        "concurrencyLimit",
                        "Min",
                        0
                },
                new Object[]{
                        "threadNamePrefix blank → ошибка @NotBlank",
                        new String[]{
                                VIRTUAL_THREADS_ENABLED + "=false",
                                CONCURRENCY_LIMIT + "=4",
                                THREAD_NAME_PREFIX + "=   "
                        },
                        "threadNamePrefix",
                        "NotBlank",
                        "" // после тримминга rejectedValue будет пустой строкой
                }
        );
    }

    /**
     * Поиск причины указанного типа в цепочке исключений.
     */
    private static <T extends Throwable> T findCause(Throwable t, Class<T> type) {
        while (t != null) {
            if (type.isInstance(t)) return type.cast(t);
            t = t.getCause();
        }
        return null;
    }

    @EnableConfigurationProperties(JobLauncherProps.class)
    static class TestConfig {
    }

    @Nested
    @DisplayName("Позитивные сценарии")
    class Positive {

        @Test
        void should_bind_valid_properties() {
            contextRunner
                    .withPropertyValues(
                            VIRTUAL_THREADS_ENABLED + "=true",
                            CONCURRENCY_LIMIT + "=8",
                            THREAD_NAME_PREFIX + "=batch-"
                    )
                    .run(ctx -> {
                        assertThat(ctx).hasNotFailed();

                        final var props = ctx.getBean(JobLauncherProps.class);
                        assertThat(props.virtualThreadsEnabled()).isTrue();
                        assertThat(props.concurrencyLimit()).isEqualTo(8);
                        assertThat(props.threadNamePrefix()).isEqualTo("batch-");
                    });
        }

        @Test
        void should_default_virtual_threads_to_false_when_not_provided() {
            // Проверяем дефолт примитива boolean
            contextRunner
                    .withPropertyValues(
                            CONCURRENCY_LIMIT + "=1",
                            THREAD_NAME_PREFIX + "=job-"
                    )
                    .run(ctx -> {
                        assertThat(ctx).hasNotFailed();

                        final var props = ctx.getBean(JobLauncherProps.class);
                        assertThat(props.virtualThreadsEnabled()).isFalse();
                        assertThat(props.concurrencyLimit()).isEqualTo(1);
                        assertThat(props.threadNamePrefix()).isEqualTo("job-");
                    });
        }
    }

    @Nested
    @DisplayName("Негативные сценарии валидации")
    class Negative {

        @ParameterizedTest(name = "[{index}] {0}")
        @MethodSource("io.github.rxtcp.integrationcheck.configuration.properties.JobLauncherPropsTest#invalidCases")
        void should_fail_binding_and_expose_expected_constraint_code(
                String caseName,
                String[] propertyValues,
                String expectedField,
                String expectedConstraintCode,
                Object expectedRejectedValue
        ) {
            contextRunner
                    .withPropertyValues(propertyValues)
                    .run(ctx -> {
                        assertThat(ctx).hasFailed();

                        // Достаём BindValidationException из chain
                        final var bve = findCause(ctx.getStartupFailure(), BindValidationException.class);
                        assertThat(bve).as("ожидаем BindValidationException").isNotNull();

                        // Ищем связанный FieldError по целевому полю
                        final var fieldError = bve.getValidationErrors().getAllErrors().stream()
                                .filter(FieldError.class::isInstance)
                                .map(FieldError.class::cast)
                                .filter(e -> e.getField().equals(expectedField))
                                .findFirst()
                                .orElse(null);

                        assertThat(fieldError).as("FieldError по полю '%s'", expectedField).isNotNull();
                        assertThat(Arrays.asList(fieldError.getCodes()))
                                .as("коды валидации должны содержать %s", expectedConstraintCode)
                                .contains(expectedConstraintCode);

                        // rejectedValue полезно сверять, но не через локализованные сообщения
                        if (expectedRejectedValue != null) {
                            if (expectedRejectedValue instanceof String) {
                                assertThat(String.valueOf(fieldError.getRejectedValue()))
                                        .isEqualTo(expectedRejectedValue);
                            } else {
                                assertThat(fieldError.getRejectedValue())
                                        .isEqualTo(expectedRejectedValue);
                            }
                        }
                    });
        }
    }
}
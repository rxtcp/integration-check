package io.github.rxtcp.integrationcheck.configuration.properties;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты биндинга и валидации свойств {@link FlywayDataSourceProps}.
 * <p>
 * Цели:
 * 1) Корректный набор свойств должен успешно биндиться в бин конфигурации.
 * 2) Нарушение ограничений валидации (NotBlank/NotNull) приводит к ошибке биндинга.
 */
@DisplayName("FlywayDataSourceProps: биндинг и валидация")
@DisplayNameGeneration(ReplaceUnderscores.class)
class FlywayDataSourcePropsTest {

    private static final String PREFIX = "application.flyway.datasource";
    private static final String URL = PREFIX + ".url";
    private static final String USERNAME = PREFIX + ".username";
    private static final String PASSWORD = PREFIX + ".password";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    /**
     * Негативные сценарии:
     * - отсутствует пароль → @NotNull на password;
     * - username пустой/пробельный → @NotBlank;
     * - url пустой/пробельный → @NotBlank.
     */
    static Stream<Object[]> invalidCases() {
        return Stream.of(
                new Object[]{
                        "Отсутствует password (@NotNull)",
                        new String[]{
                                URL + "=jdbc:postgresql://localhost:5432/db",
                                USERNAME + "=migrator"
                                // PASSWORD отсутствует
                        }
                },
                new Object[]{
                        "username пустой/пробельный (@NotBlank)",
                        new String[]{
                                URL + "=jdbc:postgresql://localhost:5432/db",
                                USERNAME + "=   ",
                                PASSWORD + "=secret"
                        }
                },
                new Object[]{
                        "url пустой/пробельный (@NotBlank)",
                        new String[]{
                                URL + "=   ",
                                USERNAME + "=migrator",
                                PASSWORD + "=secret"
                        }
                }
        );
    }

    @Test
    void should_bind_valid_properties() {
        contextRunner
                .withPropertyValues(
                        URL + "=jdbc:postgresql://localhost:5432/db?prepareThreshold=0",
                        USERNAME + "=migrator",
                        PASSWORD + "=secret"
                )
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    assertThat(ctx).hasSingleBean(FlywayDataSourceProps.class);

                    final var props = ctx.getBean(FlywayDataSourceProps.class);
                    assertThat(props.url()).isEqualTo("jdbc:postgresql://localhost:5432/db?prepareThreshold=0");
                    assertThat(props.username()).isEqualTo("migrator");
                    assertThat(props.password()).isEqualTo("secret");
                });
    }

    @Test
    void should_allow_blank_password_when_only_NotNull_is_enforced() {
        // Пустая строка допустима для @NotNull: свойство присутствует, но пустое
        contextRunner
                .withPropertyValues(
                        URL + "=jdbc:postgresql://localhost:5432/db",
                        USERNAME + "=migrator",
                        PASSWORD + "="
                )
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    final var props = ctx.getBean(FlywayDataSourceProps.class);
                    assertThat(props.password()).isEqualTo("");
                });
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidCases")
    void should_fail_binding_when_validation_constraints_are_violated(String caseName, String[] propertyValues) {
        contextRunner
                .withPropertyValues(propertyValues)
                .run(ctx -> {
                    // ожидаем провал старта контекста из-за ошибок валидации
                    assertThat(ctx).hasFailed();
                    assertThat(ctx.getStartupFailure())
                            .hasRootCauseInstanceOf(BindValidationException.class);
                });
    }

    @Configuration
    @EnableConfigurationProperties(FlywayDataSourceProps.class)
    static class TestConfig {
    }
}
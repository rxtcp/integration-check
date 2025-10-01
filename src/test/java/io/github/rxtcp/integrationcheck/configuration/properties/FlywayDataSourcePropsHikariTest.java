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
 * Тесты биндинга и валидации свойств {@link FlywayDataSourceProps.Hikari}.
 * <p>
 * Цели:
 * 1) Корректный набор свойств должен успешно биндиться в бин конфигурации.
 * 2) Нарушение ограничений валидации должно приводить к ошибке биндинга.
 */
@DisplayName("FlywayDataSourceProps.Hikari: биндинг и валидация")
@DisplayNameGeneration(ReplaceUnderscores.class)
class FlywayDataSourcePropsHikariTest {

    private static final String PREFIX = "application.flyway.datasource.hikari";
    private static final String POOL_NAME = PREFIX + ".pool-name";
    private static final String MAX_POOL_SIZE = PREFIX + ".maximum-pool-size";
    private static final String MIN_IDLE = PREFIX + ".minimum-idle";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    /**
     * Набор негативных сценариев:
     * - пустое/пробельное имя пула нарушает @NotBlank;
     * - максимальный размер пула < 1 нарушает @Min(1);
     * - минимальное число idle < 0 нарушает @Min(0).
     */
    static Stream<Object[]> invalidCases() {
        return Stream.of(
                new Object[]{"Пустой pool-name", "   ", "2", "0"},
                new Object[]{"maximum-pool-size < 1", "flyway", "0", "0"},
                new Object[]{"minimum-idle < 0", "flyway", "2", "-1"}
        );
    }

    @Test
    void should_bind_valid_hikari_properties() {
        contextRunner
                .withPropertyValues(
                        POOL_NAME + "=flyway-pool",
                        MAX_POOL_SIZE + "=3",
                        MIN_IDLE + "=0"
                )
                .run(ctx -> {
                    // контекст поднимается и бин доступен
                    assertThat(ctx).hasNotFailed();
                    assertThat(ctx).hasSingleBean(FlywayDataSourceProps.Hikari.class);

                    final var hikari = ctx.getBean(FlywayDataSourceProps.Hikari.class);

                    assertThat(hikari.poolName()).isEqualTo("flyway-pool");
                    assertThat(hikari.maximumPoolSize()).isEqualTo(3);
                    assertThat(hikari.minimumIdle()).isEqualTo(0);
                });
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidCases")
    void should_fail_binding_when_validation_constraints_are_violated(
            String caseName, String poolName, String maxPool, String minIdle
    ) {
        contextRunner
                .withPropertyValues(
                        POOL_NAME + "=" + poolName,
                        MAX_POOL_SIZE + "=" + maxPool,
                        MIN_IDLE + "=" + minIdle
                )
                .run(ctx -> {
                    // ожидаем провал старта контекста из-за ошибок валидации
                    assertThat(ctx).hasFailed();
                    assertThat(ctx.getStartupFailure())
                            .hasRootCauseInstanceOf(BindValidationException.class);
                });
    }

    @Configuration
    @EnableConfigurationProperties(FlywayDataSourceProps.Hikari.class)
    static class TestConfig {
    }
}
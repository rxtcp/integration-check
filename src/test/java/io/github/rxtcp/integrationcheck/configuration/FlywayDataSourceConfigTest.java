package io.github.rxtcp.integrationcheck.configuration;

import com.zaxxer.hikari.HikariDataSource;
import io.github.rxtcp.integrationcheck.configuration.properties.FlywayDataSourceProps;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.flyway.FlywayDataSource;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Тесты для {@link FlywayDataSourceConfig#flywayDataSource(FlywayDataSourceProps, FlywayDataSourceProps.Hikari)}.
 * <p>
 * Цель набора тестов:
 * 1) Убедиться, что метод конфигурации создаёт и настраивает пул Hikari согласно переданным пропсам.
 * 2) Убедиться, что метод помечен необходимыми аннотациями Spring.
 * <p>
 * NB: Здесь мы тестируем чистую Java-конфигурацию без запуска контекста Spring — это быстрый и надёжный юнит-тест.
 */
@DisplayName("FlywayDataSourceConfig")
@DisplayNameGeneration(ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class FlywayDataSourceConfigTest {

    @Test
    void flywayDataSource_should_create_and_configure_HikariDataSource() {
        // given: подготавливаем моки пропсов
        final var dataSourceProps = mock(FlywayDataSourceProps.class);
        when(dataSourceProps.url()).thenReturn("jdbc:h2:mem:fw;DB_CLOSE_DELAY=-1");
        when(dataSourceProps.username()).thenReturn("sa");
        when(dataSourceProps.password()).thenReturn("");

        final var hikariProps = mock(FlywayDataSourceProps.Hikari.class);
        when(hikariProps.poolName()).thenReturn("flyway-pool");
        when(hikariProps.maximumPoolSize()).thenReturn(7);
        when(hikariProps.minimumIdle()).thenReturn(2);

        final var config = new FlywayDataSourceConfig();

        // when: создаём DataSource через тестируемый метод
        final DataSource ds = config.flywayDataSource(dataSourceProps, hikariProps);

        // then: убеждаемся, что это Hikari и параметры применены
        assertThat(ds).isInstanceOf(HikariDataSource.class);

        // Используем try-with-resources, чтобы гарантированно закрыть пул.
        final HikariDataSource hk = (HikariDataSource) ds;
        try (hk) {
            assertThat(hk.getJdbcUrl()).isEqualTo("jdbc:h2:mem:fw;DB_CLOSE_DELAY=-1");
            assertThat(hk.getUsername()).isEqualTo("sa");
            assertThat(hk.getPassword()).isEqualTo("");
            assertThat(hk.getPoolName()).isEqualTo("flyway-pool");
            assertThat(hk.getMaximumPoolSize()).isEqualTo(7);
            assertThat(hk.getMinimumIdle()).isEqualTo(2);
        }

        // and: после try-with-resources пул закрыт
        assertThat(hk.isClosed()).isTrue();
    }

    @Test
    void flywayDataSource_method_should_have_required_annotations() throws NoSuchMethodException {
        // given
        final var method = FlywayDataSourceConfig.class.getDeclaredMethod(
                "flywayDataSource",
                FlywayDataSourceProps.class,
                FlywayDataSourceProps.Hikari.class
        );

        // then: метод должен быть бином и помечен как источник для Flyway
        assertThat(method.isAnnotationPresent(Bean.class)).isTrue();
        assertThat(method.isAnnotationPresent(FlywayDataSource.class)).isTrue();
    }
}
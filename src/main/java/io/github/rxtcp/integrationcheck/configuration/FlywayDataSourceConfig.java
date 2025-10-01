package io.github.rxtcp.integrationcheck.configuration;

import com.zaxxer.hikari.HikariDataSource;
import io.github.rxtcp.integrationcheck.configuration.properties.FlywayDataSourceProps;
import org.springframework.boot.autoconfigure.flyway.FlywayDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Конфигурация выделенного {@link DataSource} только для миграций Flyway.
 *
 * <p><b>Отдельный DataSource для пула соединений для миграций предотвращает конкуренцию
 * с основным пулом приложения во время старта и при ручном прогоне миграций,
 * повышает предсказуемость времени старта и уменьшает риск исчерпания соединений.</p>
 *
 * <p>Аннотация {@link FlywayDataSource} подсказывает автоконфигурации Spring Boot, что именно этот бин следует
 * использовать для выполнения миграций, не затрагивая основной {@code spring.datasource}.</p>
 *
 * <p>Экземпляр {@link HikariDataSource} управляется контейнером Spring и будет корректно закрыт
 * при остановке приложения. Ресурсы (соединения) освобождаются автоматически.</p>
 */
@Configuration
public class FlywayDataSourceConfig {

    /**
     * Создаёт и настраивает {@link HikariDataSource} для Flyway с собственными параметрами пула.
     *
     * @param dataSourceProps свойства подключения для миграций (URL, пользователь, пароль).
     * @param hikariProps     свойства пула Hikari для миграций (имя пула, размеры и т. п.).
     * @return выделенный {@link DataSource}, который используется <i>только</i> Flyway.
     */
    @Bean
    @FlywayDataSource
    DataSource flywayDataSource(FlywayDataSourceProps dataSourceProps, FlywayDataSourceProps.Hikari hikariProps) {
        var hikariDataSource = new HikariDataSource();

        hikariDataSource.setJdbcUrl(dataSourceProps.url());
        hikariDataSource.setUsername(dataSourceProps.username());
        hikariDataSource.setPassword(dataSourceProps.password());

        hikariDataSource.setPoolName(hikariProps.poolName());
        hikariDataSource.setMaximumPoolSize(hikariProps.maximumPoolSize());
        hikariDataSource.setMinimumIdle(hikariProps.minimumIdle());

        return hikariDataSource;
    }
}

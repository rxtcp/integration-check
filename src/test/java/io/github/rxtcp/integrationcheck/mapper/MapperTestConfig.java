package io.github.rxtcp.integrationcheck.mapper;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Тестовая конфигурация для подъёма MapStruct-мэпперов в контексте Spring.
 * Используем marker-классы в {@code basePackageClasses}, чтобы избежать хрупких строковых пакетов.
 */
@Configuration
@ComponentScan(basePackageClasses = {
        CheckMapper.class,
        ProfileMapper.class,
        RestApiProfileMapper.class
})
public class MapperTestConfig {
    // Здесь намеренно нет @Bean-методов: нужны только компоненты, найденные сканированием.
}

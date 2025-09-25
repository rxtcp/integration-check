package io.github.nety.integrationcheck.configuration;

import org.mapstruct.MapperConfig;

import static org.mapstruct.InjectionStrategy.CONSTRUCTOR;
import static org.mapstruct.MappingConstants.ComponentModel.SPRING;
import static org.mapstruct.NullValueCheckStrategy.ALWAYS;
import static org.mapstruct.NullValuePropertyMappingStrategy.IGNORE;
import static org.mapstruct.ReportingPolicy.ERROR;
import static org.mapstruct.ReportingPolicy.WARN;

/**
 * Базовая MapStruct-конфигурация для всех мапперов проекта.
 *
 * <ul>
 *   <li>{@code componentModel = SPRING} — генерируем Spring-бины мапперов.</li>
 *   <li>{@code injectionStrategy = CONSTRUCTOR} — внедрение зависимостей через конструктор.</li>
 *   <li>{@code unmappedTargetPolicy = ERROR} — падать при не покрытых целевых полях.</li>
 *   <li>{@code unmappedSourcePolicy = WARN} — предупреждать о неиспользуемых полях источника.</li>
 *   <li>{@code nullValuePropertyMappingStrategy = IGNORE} — не затирать целевые поля {@code null}-ами.</li>
 *   <li>{@code nullValueCheckStrategy = ALWAYS} — всегда генерировать проверки на {@code null}.</li>
 * </ul>
 * <p>
 * Наследуйте эту конфигурацию через {@code @Mapper(config = MappingConfig.class)}.
 */
@MapperConfig(
        componentModel = SPRING,
        injectionStrategy = CONSTRUCTOR,
        unmappedTargetPolicy = ERROR,
        unmappedSourcePolicy = WARN,
        nullValuePropertyMappingStrategy = IGNORE,
        nullValueCheckStrategy = ALWAYS
)
public interface MappingConfig {
}

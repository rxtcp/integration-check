package io.github.nety.integrationcheck.mapper;

import io.github.nety.integrationcheck.configuration.MappingConfig;
import io.github.nety.integrationcheck.dto.RestApiProfileDto;
import io.github.nety.integrationcheck.entity.RestApiProfile;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Маппер профиля REST-проверки в {@link RestApiProfileDto}.
 *
 * <p>Использует общую конфигурацию {@link MappingConfig}: Spring-бин, конструкторная инъекция,
 * строгий контроль незамапленных полей.</p>
 */
@Mapper(
        config = MappingConfig.class
)
public interface RestApiProfileMapper {

    /**
     * Проецирует {@link RestApiProfile} в {@link RestApiProfileDto}.
     *
     * <ul>
     *   <li>{@code ignoreByDefault = true} — маппятся только явно перечисленные поля.</li>
     *   <li>{@code checkId} берётся из связанной сущности {@code src.getCheck().getId()}.</li>
     *   <li>Остальные поля копируются напрямую.</li>
     * </ul>
     *
     * <strong>Замечание:</strong> связь {@code src.getCheck()} должна быть инициализирована
     * (учтите ленивую загрузку), иначе возможен {@link NullPointerException}.
     *
     * @param src исходная сущность профиля
     * @return DTO представление профиля
     */
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "checkId", expression = "java(src.getCheck().getId())")
    @Mapping(target = "profileId", source = "id")
    @Mapping(target = "url", source = "url")
    @Mapping(target = "httpMethod", source = "httpMethod")
    @Mapping(target = "timeoutSeconds", source = "timeoutSeconds")
    @Mapping(target = "headers", source = "headers")
    @Mapping(target = "requestBody", source = "requestBody")
    @Mapping(target = "expectedHttpCode", source = "expectedHttpCode")
    RestApiProfileDto toDto(RestApiProfile src);
}

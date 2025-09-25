package io.github.nety.integrationcheck.mapper;

import io.github.nety.integrationcheck.configuration.MappingConfig;
import io.github.nety.integrationcheck.dto.CheckProfileDto;
import io.github.nety.integrationcheck.dto.RestApiProfileDto;
import io.github.nety.integrationcheck.entity.CheckProfile;
import io.github.nety.integrationcheck.entity.RestApiProfile;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.SubclassMapping;

/**
 * Полиморфный маппер профилей проверки в DTO.
 *
 * <p>Наследует строгую конфигурацию из {@link MappingConfig} и делегирует
 * маппинг вложенных структур в {@link RestApiProfileMapper}.</p>
 */
@Mapper(
        config = MappingConfig.class,
        uses = {RestApiProfileMapper.class}
)
public interface ProfileMapper {

    /**
     * Проецирует {@link RestApiProfile} в {@link RestApiProfileDto}.
     *
     * <ul>
     *   <li>{@code ignoreByDefault = true} — маппятся только явно перечисленные поля.</li>
     *   <li>{@code checkId} берётся из связанной сущности {@code src.getCheck().getId()}.</li>
     *   <li>{@code profileId} маппится из идентификатора профиля.</li>
     * </ul>
     *
     * <strong>Предусловие:</strong> связь {@code src.getCheck()} должна быть инициализирована
     * (учитывайте ленивую загрузку).
     */
    @SubclassMapping(source = RestApiProfile.class, target = RestApiProfileDto.class)
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "checkId", expression = "java(src.getCheck().getId())")
    @Mapping(target = "profileId", source = "id")
    RestApiProfileDto toDto(RestApiProfile src);

    /**
     * Унифицированная точка входа для базового типа профиля.
     * <p>Поддерживает только известные специализации; при неизвестном типе — fail-fast.</p>
     *
     * @throws IllegalStateException для неподдерживаемых реализаций {@link CheckProfile}.
     */
    default CheckProfileDto toDto(CheckProfile checkProfile) {
        if (checkProfile instanceof RestApiProfile restApiProfile) {
            return toDto(restApiProfile);
        }
        throw new IllegalStateException("Неподдерживаемый тип профиля: " + checkProfile.getClass());
    }
}

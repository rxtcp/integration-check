package io.github.rxtcp.integrationcheck.mapper;

import io.github.rxtcp.integrationcheck.configuration.MappingConfig;
import io.github.rxtcp.integrationcheck.dto.CheckDto;
import io.github.rxtcp.integrationcheck.entity.Check;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Маппер доменной сущности {@link Check} в транспортную модель {@link CheckDto}.
 * <p>
 * Конфигурация наследуется из {@link MappingConfig}: Spring-бин, конструкторная инъекция,
 * строгая проверка неотмапленных полей. Вложенный объект маппится через {@link ProfileMapper}.
 */
@Mapper(
        config = MappingConfig.class,
        uses = ProfileMapper.class
)
public interface CheckMapper {

    /**
     * Проецирует {@link Check} в {@link CheckDto}.
     * <p>
     * {@code ignoreByDefault = true} — явно перечислены только нужные поля, остальные игнорируются.
     * При добавлении новых полей в целевую модель обновите маппинг (иначе сработает политика из {@code MappingConfig}).
     *
     * @param entity исходная сущность
     * @return целевой DTO
     */
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "enabled", source = "enabled")
    @Mapping(target = "runIntervalMin", source = "runIntervalMin")
    @Mapping(target = "nextRunAt", source = "nextRunAt")
    @Mapping(target = "type", source = "type")
    @Mapping(target = "profile", source = "profile")
    CheckDto toDto(Check entity);
}

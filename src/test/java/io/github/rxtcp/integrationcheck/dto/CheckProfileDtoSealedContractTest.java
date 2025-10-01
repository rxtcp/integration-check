package io.github.rxtcp.integrationcheck.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Проверяем контракт sealed-типа {@link CheckProfileDto}.
 * <p>
 * Цели:
 * 1) Тип действительно помечен как sealed.
 * 2) Среди разрешённых подклассов присутствует {@link RestApiProfileDto}.
 * <p>
 * Примечание: используем рефлексию JDK 21: {@code Class#isSealed()} и {@code Class#getPermittedSubclasses()}.
 */
@DisplayName("CheckProfileDto: контракт sealed")
@DisplayNameGeneration(ReplaceUnderscores.class)
class CheckProfileDtoSealedContractTest {

    @Test
    void should_be_sealed_and_permit_RestApiProfileDto() {
        // given
        final Class<CheckProfileDto> sealedType = CheckProfileDto.class;

        // when
        final Set<Class<?>> permitted = Arrays.stream(sealedType.getPermittedSubclasses())
                .collect(Collectors.toSet());

        // then
        // 1) Сам тип — sealed
        assertThat(sealedType.isSealed()).isTrue();

        // 2) Среди разрешённых подклассов есть ожидаемый DTO
        assertThat(permitted)
                .as("разрешённые подклассы должны включать RestApiProfileDto")
                .contains(RestApiProfileDto.class);

        // 3) Дополнительно фиксируем отношение наследования
        assertThat(CheckProfileDto.class).isAssignableFrom(RestApiProfileDto.class);
    }
}
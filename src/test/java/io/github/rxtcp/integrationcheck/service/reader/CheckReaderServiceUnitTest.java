package io.github.rxtcp.integrationcheck.service.reader;

import io.github.rxtcp.integrationcheck.entity.Check;
import io.github.rxtcp.integrationcheck.repository.CheckRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("CheckReaderService — чтение идентификаторов и сущностей")
@DisplayNameGeneration(ReplaceUnderscores.class)
class CheckReaderServiceUnitTest {

    // Константы для читаемости и стабильности
    private static final long FOUND_ID = 42L;
    private static final long MISSING_ID = 777L;

    @Mock
    CheckRepository checkRepository;

    @InjectMocks
    CheckReaderService service;

    @Test
    @DisplayName("findDueIds — делегирует в репозиторий и возвращает список id без изменений")
    void should_delegate_findDueIds_to_repository_and_return_ids() {
        // given
        given(checkRepository.findDueCheckIds()).willReturn(List.of(1L, 2L, 3L));

        // when
        List<Long> ids = service.findDueIds();

        // then
        assertThat(ids).containsExactly(1L, 2L, 3L);
        then(checkRepository).should().findDueCheckIds();
        then(checkRepository).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("findWithProfileById — при наличии возвращает найденную сущность как есть")
    void should_return_entity_when_found_by_id() {
        // given
        Check check = new Check();
        check.setId(FOUND_ID);
        given(checkRepository.findWithProfileById(FOUND_ID)).willReturn(Optional.of(check));

        // when
        Check result = service.findWithProfileById(FOUND_ID);

        // then
        assertThat(result).isSameAs(check);
        then(checkRepository).should().findWithProfileById(FOUND_ID);
        then(checkRepository).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("findWithProfileById — при отсутствии бросает EntityNotFoundException с id в сообщении")
    void should_throw_EntityNotFoundException_when_entity_not_found() {
        // given
        given(checkRepository.findWithProfileById(MISSING_ID)).willReturn(Optional.empty());

        // when / then
        assertThatExceptionOfType(EntityNotFoundException.class)
                .isThrownBy(() -> service.findWithProfileById(MISSING_ID))
                .withMessageContaining("id=" + MISSING_ID);

        then(checkRepository).should().findWithProfileById(MISSING_ID);
        then(checkRepository).shouldHaveNoMoreInteractions();
    }
}
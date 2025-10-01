package io.github.rxtcp.integrationcheck.entity;

import io.github.rxtcp.integrationcheck.enums.CheckType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static io.github.rxtcp.integrationcheck.entity.EntityTestFixtures.newCheck;
import static io.github.rxtcp.integrationcheck.entity.EntityTestFixtures.newRestProfile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Юнит-тесты для сервиса, который прикрепляет профиль к {@link Check} и сохраняет его.
 * <p>
 * Цели:
 * 1) Профиль прикрепляется к чек-сущности, тип проставляется согласно аргументу.
 * 2) Делегирование в {@code Saver.save(...)} выполняется ровно один раз.
 * 3) Возвращается тот же объект, который был передан в saver (как в typical repository stub).
 */
@DisplayName("CheckService: attach profile + save")
@DisplayNameGeneration(ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class CheckServiceMockTest {

    @Mock
    private Saver saver;

    @ParameterizedTest(name = "[{index}] type={0}")
    @EnumSource(CheckType.class)
    void should_attach_profile_set_type_and_delegate_save(CheckType type) {
        // given
        final CheckService service = new CheckService(saver);
        final Check check = newCheck("svc");
        final RestApiProfile profile = newRestProfile();

        // репозиторий возвращает тот же экземпляр, что получил
        when(saver.save(any())).thenAnswer(inv -> inv.getArgument(0, Check.class));

        // when
        final Check returned = service.createWithProfile(check, profile, type);

        // then: делегирование и что именно сохранили
        final ArgumentCaptor<Check> captor = ArgumentCaptor.forClass(Check.class);
        verify(saver, times(1)).save(captor.capture());
        verifyNoMoreInteractions(saver);

        final Check saved = captor.getValue();

        // профиль прикреплён к чек-сущности (двунаправленно), и тип выставлен
        assertThat(saved.getProfile()).isSameAs(profile);
        assertThat(profile.getCheck()).isSameAs(saved);
        assertThat(saved.getType()).isEqualTo(type);

        // сервис возвращает тот же экземпляр, который ушёл в saver
        assertThat(returned).isSameAs(saved);
    }

    @Test
    void should_not_mutate_profile_reference_object_itself() {
        // Дополнительная гарантия: сервис не подменяет объект профиля, а использует переданный экземпляр.
        final CheckService service = new CheckService(saver);
        final Check check = newCheck("svc");
        final RestApiProfile profile = newRestProfile();

        when(saver.save(any())).thenAnswer(inv -> inv.getArgument(0, Check.class));

        final Check saved = service.createWithProfile(check, profile, CheckType.values()[0]);

        assertThat(saved.getProfile()).isSameAs(profile);
        assertThat(profile.getCheck()).isSameAs(saved);
    }

    /**
     * Абстракция хранилища/репозитория.
     */
    interface Saver {
        Check save(Check c);
    }

    /**
     * Тестируемый сервис.
     */
    static class CheckService {
        private final Saver saver;

        CheckService(Saver saver) {
            this.saver = saver;
        }

        public Check createWithProfile(Check check, RestApiProfile profile, CheckType type) {
            check.attachProfile(profile, type);
            return saver.save(check);
        }
    }
}
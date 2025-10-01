package io.github.rxtcp.integrationcheck.entity.support;

import io.github.rxtcp.integrationcheck.common.contract.Identifiable;
import org.hibernate.Hibernate;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Тесты для {@link HibernateEntityUtil}.
 * <p>
 * Цели:
 * 1) {@code idOf(...)} корректно извлекает id из обычных сущностей и Hibernate-прокси.
 * 2) {@code idOf(...)} отклоняет неподдерживаемые типы и не-Long id.
 * 3) {@code simpleClassName(...)} корректно даёт человекочитаемое имя класса, в т.ч. для прокси.
 */
@DisplayName("HibernateEntityUtil")
@DisplayNameGeneration(ReplaceUnderscores.class)
class HibernateEntityUtilTest {

    // Простейшая сущность с Long-id
    private static final class DummyEntity implements Identifiable<Long> {
        private final Long id;

        DummyEntity(Long id) {
            this.id = id;
        }

        @Override
        public Long getId() {
            return id;
        }
    }

    // Специально «плохая» сущность с not-Long id, чтобы проверить ClassCastException
    private static final class StringIdEntity implements Identifiable<String> {
        private final String id;

        StringIdEntity(String id) {
            this.id = id;
        }

        @Override
        public String getId() {
            return id;
        }
    }

    @Nested
    @DisplayName("idOf(...)")
    class IdOfTests {

        @Test
        void should_return_null_when_entity_is_null() {
            assertThat(HibernateEntityUtil.idOf(null)).isNull();
        }

        @Test
        void should_return_id_when_entity_is_identifiable_long() {
            var entity = new DummyEntity(123L);

            assertThat(HibernateEntityUtil.idOf(entity)).isEqualTo(123L);
        }

        @Test
        void should_return_identifier_from_lazy_initializer_when_entity_is_hibernate_proxy() {
            HibernateProxy proxy = mock(HibernateProxy.class);
            LazyInitializer initializer = mock(LazyInitializer.class);
            when(proxy.getHibernateLazyInitializer()).thenReturn(initializer);
            when(initializer.getIdentifier()).thenReturn(777L);

            Long id = HibernateEntityUtil.idOf(proxy);

            assertThat(id).isEqualTo(777L);
            verify(proxy).getHibernateLazyInitializer();
            verify(initializer).getIdentifier();
            verifyNoMoreInteractions(proxy, initializer);
        }

        @Test
        void should_throw_illegal_argument_when_type_is_not_supported() {
            assertThatThrownBy(() -> HibernateEntityUtil.idOf("not-entity"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void should_throw_class_cast_when_identifiable_id_is_not_long() {
            var bad = new StringIdEntity("abc");

            assertThatThrownBy(() -> HibernateEntityUtil.idOf(bad))
                    .isInstanceOf(ClassCastException.class);
        }
    }

    @Nested
    @DisplayName("simpleClassName(...)")
    class SimpleClassNameTests {

        @Test
        void should_return_string_null_when_object_is_null() {
            assertThat(HibernateEntityUtil.simpleClassName(null)).isEqualTo("null");
        }

        @Test
        void should_return_plain_simple_name_for_usual_entity() {
            assertThat(HibernateEntityUtil.simpleClassName(new DummyEntity(1L)))
                    .isEqualTo("DummyEntity");
        }

        @Test
        void should_return_underlying_simple_name_for_hibernate_proxy_using_Hibernate_getClass() {
            // любой объект, представляющий «прокси». Важен именно вызов Hibernate.getClass(...)
            Object proxyLike = new Object();

            try (MockedStatic<Hibernate> hibernate = mockStatic(Hibernate.class)) {
                hibernate.when(() -> Hibernate.getClass(proxyLike)).thenReturn(DummyEntity.class);

                assertThat(HibernateEntityUtil.simpleClassName(proxyLike)).isEqualTo("DummyEntity");

                // проверяем, что обратились именно к Hibernate.getClass
                hibernate.verify(() -> Hibernate.getClass(proxyLike));
            }
        }
    }
}
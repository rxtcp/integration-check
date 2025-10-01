package io.github.rxtcp.integrationcheck.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.github.rxtcp.integrationcheck.entity.EntityTestFixtures.newCheck;
import static io.github.rxtcp.integrationcheck.entity.EntityTestFixtures.newRestProfile;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Контракт equals/hashCode и toString для {@link RestApiProfile}.
 * <p>
 * Предпосылки реализации:
 * - equals сравнивает по идентификатору (Long) и реальному классу (через Hibernate.getClass(...) в базовом классе).
 * - hashCode зависит от фактического класса (до назначения id может совпадать у разных экземпляров).
 * - toString содержит ключевые поля профиля.
 */
@DisplayName("RestApiProfile: контракт equals/hashCode и toString")
@DisplayNameGeneration(ReplaceUnderscores.class)
class RestApiProfileCoreLogicTest {

    private static final long ID_10 = 10L;
    private static final long ID_11 = 11L;

    // Небольшие фабрики для читаемости
    private static RestApiProfile profile(Long id) {
        final RestApiProfile p = newRestProfile();
        p.setId(id);
        return p;
    }

    @Nested
    @DisplayName("equals/hashCode")
    class EqualsHashCodeContract {

        @Test
        void should_be_equal_and_have_same_hashcode_when_ids_equal_and_classes_match() {
            final RestApiProfile left = profile(ID_10);
            final RestApiProfile right = profile(ID_10);

            assertThat(left).isEqualTo(right);
            assertThat(right).isEqualTo(left); // симметрия
            assertThat(left.hashCode()).isEqualTo(right.hashCode()); // равным объектам — равный hashCode
        }

        @Test
        void should_not_be_equal_when_ids_differ_or_id_is_null() {
            final RestApiProfile a = profile(ID_10);
            final RestApiProfile b = profile(ID_11);
            final RestApiProfile c = profile(null);

            assertThat(a).isNotEqualTo(b);
            assertThat(a).isNotEqualTo(c);
        }

        @Test
        void should_have_same_hashcode_for_same_class_even_when_ids_differ() {
            final RestApiProfile x = profile(100L);
            final RestApiProfile y = profile(200L);

            // hashCode зависит от класса, а не от id
            assertThat(x.hashCode()).isEqualTo(y.hashCode());
            // но equals при разных id — false
            assertThat(x).isNotEqualTo(y);
        }
    }

    @Nested
    @DisplayName("toString()")
    class ToStringContract {

        @Test
        void should_contain_key_fields() {
            final RestApiProfile p = newRestProfile();
            p.setId(5L);

            final Check check = newCheck("n");
            check.setId(1L);
            p.setCheck(check);

            final String s = p.toString();

            assertThat(s).contains(
                    "id=5",
                    "checkId=1",
                    "url='https://example.org/health'",
                    "httpMethod=",
                    "timeoutSeconds=5",
                    "expectedHttpCode=200"
            );
        }
    }
}
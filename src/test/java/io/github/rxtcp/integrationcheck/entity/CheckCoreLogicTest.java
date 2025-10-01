package io.github.rxtcp.integrationcheck.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Контракт equals/hashCode для {@link Check}.
 * <p>
 * Предпосылки реализации:
 * - equals сравнивает по идентификатору (Long) и реальному классу через Hibernate.getClass(..).
 * - hashCode зависит только от Hibernate-класса (стабилен до назначения id).
 */
@DisplayName("Check: контракт equals/hashCode")
@DisplayNameGeneration(ReplaceUnderscores.class)
class CheckCoreLogicTest {

    // --- Вспомогательные фабрики для читаемости ---
    private static Check check(Long id) {
        final Check c = new Check();
        c.setId(id);
        return c;
    }

    private static MyCheck myCheck(Long id) {
        final MyCheck c = new MyCheck();
        c.setId(id);
        return c;
    }

    /**
     * Вспомогательный подкласс для проверки семантики Hibernate.getClass(..) в equals/hashCode.
     */
    static class MyCheck extends Check { /* без доп. логики */
    }

    @Nested
    @DisplayName("equals(...)")
    class EqualsContract {

        @Test
        void should_be_true_and_hashcodes_equal_when_same_id_and_same_class() {
            final Check left = check(1L);
            final Check right = check(1L);

            assertThat(left).isEqualTo(right);
            assertThat(right).isEqualTo(left);
            // равные объекты обязаны иметь одинаковый hashCode
            assertThat(left.hashCode()).isEqualTo(right.hashCode());
        }

        @Test
        void should_be_false_when_ids_differ() {
            final Check left = check(1L);
            final Check right = check(2L);

            assertThat(left).isNotEqualTo(right);
            assertThat(right).isNotEqualTo(left);
        }

        @Test
        void should_be_false_when_both_ids_are_null() {
            final Check left = check(null);
            final Check right = check(null);

            // две «новые» сущности без id не считаются равными
            assertThat(left).isNotEqualTo(right);
            assertThat(right).isNotEqualTo(left);
        }

        @Test
        void should_be_false_when_compared_with_null_or_different_type() {
            final Check entity = check(1L);

            assertThat(entity).isNotEqualTo(null);
            assertThat(entity).isNotEqualTo("not-check");
        }

        @Test
        void should_be_false_when_same_id_but_different_runtime_class() {
            final Check base = check(5L);
            final MyCheck sub = myCheck(5L);

            // equals использует Hibernate.getClass(..) => разные классы → false в обе стороны
            assertThat(base).isNotEqualTo(sub);
            assertThat(sub).isNotEqualTo(base);
        }

        @Test
        void should_be_reflexive_and_consistent() {
            final Check entity = check(10L);

            // рефлексивность
            assertThat(entity).isEqualTo(entity);

            // консистентность (в отсутствие изменений состояние сравнения стабильно)
            assertThat(entity).isEqualTo(entity);
            assertThat(entity.hashCode()).isEqualTo(entity.hashCode());
        }
    }

    @Nested
    @DisplayName("hashCode()")
    class HashCodeContract {

        @Test
        void should_depend_only_on_hibernate_class() {
            final Check base = check(null);
            final MyCheck sub = myCheck(null);

            // hashCode() = Hibernate.getClass(this).hashCode()
            assertThat(base.hashCode()).isEqualTo(Check.class.hashCode());
            assertThat(sub.hashCode()).isEqualTo(MyCheck.class.hashCode());
            assertThat(base.hashCode()).isNotEqualTo(sub.hashCode());
        }
    }
}
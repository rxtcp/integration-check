package io.github.rxtcp.integrationcheck.entity;

import io.github.rxtcp.integrationcheck.enums.CheckRunStatus;
import io.github.rxtcp.integrationcheck.enums.FailureReason;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Контракт equals/hashCode/toString и builder для {@link CheckResult}.
 * <p>
 * Предпосылки реализации:
 * - equals сравнивает по идентификатору (Long) и реальному классу (через Hibernate.getClass(...) в самой сущности).
 * - hashCode зависит только от фактического класса (до назначения id может совпадать у разных сущностей).
 * - toString содержит ключевые поля результата.
 */
@DisplayName("CheckResult: контракт equals/hashCode/toString и builder")
@DisplayNameGeneration(ReplaceUnderscores.class)
class CheckResultCoreLogicTest {

    private static final long ID_1 = 1L;
    private static final long ID_2 = 2L;
    private static final long CHECK_ID = 5L;

    private static final LocalDateTime STARTED_AT = LocalDateTime.of(2024, 1, 2, 3, 4, 5);
    private static final LocalDateTime FINISHED_AT = LocalDateTime.of(2024, 1, 2, 3, 5, 6);

    // ----- Небольшие фабрики для читаемости -----

    private static CheckResult checkResult(Long id) {
        final CheckResult r = new CheckResult();
        r.setId(id);
        return r;
    }

    private static Check checkWithId(long id) {
        final Check c = new Check();
        c.setId(id);
        return c;
    }

    // ===== equals/hashCode =====

    @Nested
    @DisplayName("equals/hashCode")
    class EqualsHashCodeContract {

        @Test
        void should_be_equal_and_have_same_hashcode_when_ids_equal_and_classes_match() {
            final CheckResult left = checkResult(ID_1);
            final CheckResult right = checkResult(ID_1);

            assertThat(left).isEqualTo(right);
            assertThat(right).isEqualTo(left); // симметрия
            assertThat(left.hashCode()).isEqualTo(right.hashCode()); // равным объектам — равный hashCode
        }

        @Test
        void should_not_be_equal_when_ids_differ_or_id_is_null() {
            final CheckResult a = checkResult(ID_1);
            final CheckResult b = checkResult(ID_2);
            final CheckResult c = checkResult(null);
            final CheckResult d = checkResult(null);

            assertThat(a).isNotEqualTo(b);
            assertThat(a).isNotEqualTo(c);
            assertThat(c).isNotEqualTo(d); // обе «новые» сущности без id не равны
            assertThat(a).isNotEqualTo("string"); // другой тип
            assertThat(a).isNotEqualTo(null);     // null
            assertThat(a).isEqualTo(a);           // рефлексивность
        }

        @Test
        void should_have_same_hashcode_for_same_class_even_when_ids_differ() {
            final CheckResult x = checkResult(10L);
            final CheckResult y = checkResult(20L);

            // hashCode зависит от класса, а не от id
            assertThat(x.hashCode()).isEqualTo(y.hashCode());
            // но equals при разных id — false
            assertThat(x).isNotEqualTo(y);
        }
    }

    // ===== toString =====

    @Nested
    @DisplayName("toString()")
    class ToStringContract {

        @Test
        void should_contain_key_fields() {
            final Check check = checkWithId(CHECK_ID);

            final CheckResult r = new CheckResult();
            r.setId(42L);
            r.setCheck(check);
            r.setStartedAt(STARTED_AT);
            r.setFinishedAt(FINISHED_AT);
            r.setStatus(CheckRunStatus.values()[0]);     // не завязываемся на конкретное имя enum-константы
            r.setFailureReason(FailureReason.values()[0]);
            r.setDetails("detail");

            final String s = r.toString();

            assertThat(s).contains(
                    "id=42",
                    "checkId=5",
                    "startedAt=2024-01-02T03:04:05",
                    "finishedAt=2024-01-02T03:05:06",
                    "status=",
                    "failureReason=",
                    "details='detail'"
            );
        }
    }

    // ===== builder =====

    @Nested
    @DisplayName("builder()")
    class BuilderContract {

        @Test
        void should_set_all_fields() {
            final LocalDateTime started = LocalDateTime.of(2030, 1, 1, 0, 0, 0);
            final LocalDateTime finished = started.plusSeconds(1);

            final CheckResult r = CheckResult.builder()
                    .check(null)
                    .startedAt(started)
                    .finishedAt(finished)
                    .status(CheckRunStatus.values()[0])
                    .failureReason(null)
                    .details("x")
                    .build();

            assertThat(r.getCheck()).isNull();
            assertThat(r.getStartedAt()).isEqualTo(started);
            assertThat(r.getFinishedAt()).isEqualTo(finished);
            assertThat(r.getStatus()).isNotNull();
            assertThat(r.getFailureReason()).isNull();
            assertThat(r.getDetails()).isEqualTo("x");
        }
    }
}
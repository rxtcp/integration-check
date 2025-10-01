package io.github.rxtcp.integrationcheck.entity;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static io.github.rxtcp.integrationcheck.entity.ResultEntityFixtures.newResult;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Валидация bean-constraint'ов для {@link CheckResult}.
 * <p>
 * Цели:
 * 1) Валидная сущность не даёт нарушений.
 * 2) Поле {@code status} обязательно (@NotNull).
 * 3) Поля {@code startedAt}/{@code finishedAt} не проверяются Bean Validation (проверка уходит на уровень БД/JPA).
 */
@DisplayName("CheckResult: валидация сущности")
@DisplayNameGeneration(ReplaceUnderscores.class)
class CheckResultValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setup_validator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    private static void assertNoViolations(CheckResult entity) {
        assertThat(validator.validate(entity)).isEmpty();
    }

    /**
     * Проверяем по имени propertyPath — стабильно и не зависит от локали/текстов сообщений.
     */
    private static void assertHasViolationFor(String field, Set<ConstraintViolation<CheckResult>> violations) {
        assertThat(violations)
                .as("ожидаем нарушение для поля '%s'", field)
                .anySatisfy(v -> assertThat(v.getPropertyPath().toString()).isEqualTo(field));
    }

    @Nested
    @DisplayName("Позитивные сценарии")
    class PositiveCases {

        @Test
        void should_have_no_violations_when_status_present() {
            // given
            final CheckResult result = newResult();

            // then
            assertNoViolations(result);
        }
    }

    // ===== Утилиты для лаконичных и стабильных проверок =====

    @Nested
    @DisplayName("Негативные сценарии")
    class NegativeCases {

        @Test
        void should_reject_when_status_is_null() {
            // given
            final CheckResult result = newResult();
            result.setStatus(null);

            // when
            final Set<ConstraintViolation<CheckResult>> violations = validator.validate(result);

            // then
            assertHasViolationFor("status", violations);
        }
    }

    @Nested
    @DisplayName("Границы ответственности Bean Validation")
    class BeanValidationBoundaries {

        @Test
        void should_not_validate_started_and_finished_nullability() {
            // У этих полей нет @NotNull — корректность обеспечивается на уровне БД (nullable=false) и JPA-тестами.
            final CheckResult result = newResult();
            result.setStartedAt(null);
            result.setFinishedAt(null);

            assertNoViolations(result);
        }
    }
}
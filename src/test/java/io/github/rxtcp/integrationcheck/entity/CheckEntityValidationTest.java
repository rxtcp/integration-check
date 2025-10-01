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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static io.github.rxtcp.integrationcheck.entity.EntityTestFixtures.newCheck;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Валидация bean-constraint'ов для {@link Check}.
 * <p>
 * Цели:
 * 1) Валидная сущность не даёт нарушений.
 * 2) Поле name помечено @NotBlank.
 * 3) Поле runIntervalMin валидируется по границам @Min/@Max.
 */
@DisplayName("Check: валидация сущности")
@DisplayNameGeneration(ReplaceUnderscores.class)
class CheckEntityValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setup_validator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    private static void assertNoViolations(Check entity) {
        assertThat(validator.validate(entity)).isEmpty();
    }

    /**
     * Проверяем по имени propertyPath — стабильно и не зависит от локали/текстов сообщений.
     */
    private static void assertHasViolationFor(String field, Set<ConstraintViolation<Check>> violations) {
        assertThat(violations)
                .as("ожидаем нарушение для поля '%s'", field)
                .anySatisfy(v -> assertThat(v.getPropertyPath().toString()).isEqualTo(field));
    }

    @Nested
    @DisplayName("Позитивные сценарии")
    class PositiveCases {

        @Test
        void should_have_no_violations_for_valid_entity() {
            final Check entity = newCheck("UniqueName");

            assertNoViolations(entity);
        }
    }

    // ===== Утилиты для лаконичных и стабильных проверок =====

    @Nested
    @DisplayName("Поле name (@NotBlank)")
    class NameValidation {

        @ParameterizedTest(name = "[{index}] blank=''{0}''")
        @ValueSource(strings = {"  ", "\t", "\n"})
        void should_reject_blank_name(String blankName) {
            final Check entity = newCheck(blankName);

            final Set<ConstraintViolation<Check>> violations = validator.validate(entity);

            assertHasViolationFor("name", violations);
        }
    }

    @Nested
    @DisplayName("Поле runIntervalMin (@Min/@Max)")
    class RunIntervalValidation {

        @ParameterizedTest(name = "[{index}] invalid={0}")
        @ValueSource(ints = {0, 10081})
        void should_reject_values_out_of_bounds(int invalid) {
            final Check entity = newCheck("n-invalid");
            entity.setRunIntervalMin(invalid);

            assertThat(validator.validate(entity)).isNotEmpty();
        }

        @ParameterizedTest(name = "[{index}] edge={0}")
        @ValueSource(ints = {1, 10080})
        void should_accept_edge_values(int edge) {
            final Check entity = newCheck("n-edge");
            entity.setRunIntervalMin(edge);

            assertNoViolations(entity);
        }
    }
}
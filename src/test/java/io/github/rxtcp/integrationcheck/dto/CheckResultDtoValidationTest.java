package io.github.rxtcp.integrationcheck.dto;

import io.github.rxtcp.integrationcheck.enums.CheckRunStatus;
import io.github.rxtcp.integrationcheck.enums.FailureReason;
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
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты валидации {@link CheckResultDto}.
 * <p>
 * Цели:
 * 1) Валидный DTO не содержит нарушений.
 * 2) Обязательное поле {@code status} помечено как @NotNull.
 * 3) Поле {@code details} опционально и допускает любое значение (включая null и пустые строки).
 * 4) Поле {@code failureReason} опционально; любые значения enum валидны при наличии {@code status}.
 * <p>
 * Примечание: проверяем только bean-constraints, без бизнес-логики.
 */
@DisplayName("CheckResultDto: валидация bean-constraint'ов")
@DisplayNameGeneration(ReplaceUnderscores.class)
class CheckResultDtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setup_validator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    private static CheckRunStatus anyStatus() {
        // Берём любое валидное значение статуса — конкретное не важно для валидации.
        return CheckRunStatus.values()[0];
    }

    // ===== Позитивные сценарии =====

    /**
     * Утилита: нет нарушений валидации.
     */
    private static void assertNoViolations(CheckResultDto dto) {
        assertThat(validator.validate(dto)).isEmpty();
    }

    // ===== Негативные сценарии =====

    /**
     * Утилита: среди нарушений есть нарушение для указанного поля (стабильно, не зависит от локали).
     */
    private static void assertHasViolationFor(String field, Set<ConstraintViolation<CheckResultDto>> violations) {
        assertThat(violations)
                .as("ожидаем нарушение для поля '%s'", field)
                .anySatisfy(v -> assertThat(v.getPropertyPath().toString()).isEqualTo(field));
    }

    // ===== Вспомогательные методы =====

    @Nested
    @DisplayName("Позитивные сценарии")
    class PositiveCases {

        @Test
        void should_be_valid_when_status_present_and_failureReason_null_and_details_present() {
            // given
            final CheckResultDto dto = new CheckResultDto(anyStatus(), null, "ok");

            // then
            assertNoViolations(dto);
        }

        @ParameterizedTest(name = "[{index}] details=''{0}''")
        @NullSource
        @ValueSource(strings = {"", "  ", "some-details"})
        void should_allow_any_details_including_null_and_blank(String details) {
            // given
            final CheckResultDto dto = new CheckResultDto(anyStatus(), null, details);

            // then
            assertNoViolations(dto);
        }

        @ParameterizedTest(name = "[{index}] failureReason={0}")
        @EnumSource(FailureReason.class)
        void should_be_valid_when_status_present_and_failureReason_present_and_details_null(FailureReason reason) {
            // given
            final CheckResultDto dto = new CheckResultDto(anyStatus(), reason, null);

            // then
            assertNoViolations(dto);
        }
    }

    @Nested
    @DisplayName("Негативные сценарии")
    class NegativeCases {

        @Test
        void should_reject_when_status_is_null() {
            // given
            final CheckResultDto dto = new CheckResultDto(null, null, "anything");

            // when
            final Set<ConstraintViolation<CheckResultDto>> violations = validator.validate(dto);

            // then
            assertHasViolationFor("status", violations);
        }
    }
}
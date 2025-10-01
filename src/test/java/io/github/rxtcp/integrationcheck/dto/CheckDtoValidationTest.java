package io.github.rxtcp.integrationcheck.dto;

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

import static io.github.rxtcp.integrationcheck.dto.TestFixtures.anyCheckType;
import static io.github.rxtcp.integrationcheck.dto.TestFixtures.validCheckDto;
import static io.github.rxtcp.integrationcheck.dto.TestFixtures.validRestApiProfile;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты валидации DTO {@link CheckDto}.
 * <p>
 * Цели:
 * 1) Валидный DTO не должен содержать нарушений.
 * 2) Для каждого поля с ограничениями (@NotBlank/@NotNull/@Min/@Max) получаем ожидаемые нарушения.
 */
@DisplayName("CheckDto: валидация bean-constraint'ов")
@DisplayNameGeneration(ReplaceUnderscores.class)
class CheckDtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setupValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    /**
     * Утилита: проверяет, что среди нарушений есть нарушение для указанного поля.
     * Оставляем проверку на уровне имени поля — это стабильно и не зависит от локали/сообщений.
     */
    private static void assertHasViolationFor(String field, Set<ConstraintViolation<CheckDto>> violations) {
        assertThat(violations)
                .as("ожидаем нарушение для поля '%s'", field)
                .anySatisfy(v -> assertThat(v.getPropertyPath().toString()).isEqualTo(field));
    }

    @Test
    void should_accept_valid_dto() {
        // given
        final CheckDto dto = validCheckDto();

        // when
        final Set<ConstraintViolation<CheckDto>> violations = validator.validate(dto);

        // then
        assertThat(violations).isEmpty();
    }

    @Nested
    @DisplayName("Поле name (@NotBlank)")
    class NameValidation {

        @ParameterizedTest(name = "[{index}] blank='{0}'")
        @ValueSource(strings = {"   ", "\t", "\n"})
        void should_reject_blank_name(String blank) {
            // given
            final var base = validCheckDto();
            final CheckDto dto = new CheckDto(
                    1L, blank, "d", true, 5,
                    base.nextRunAt(),
                    anyCheckType(),
                    validRestApiProfile(1L)
            );

            // when
            final var violations = validator.validate(dto);

            // then
            assertHasViolationFor("name", violations);
        }
    }

    @Nested
    @DisplayName("Поле runIntervalMin (@Min/@Max)")
    class RunIntervalMinValidation {

        @ParameterizedTest(name = "[{index}] invalid={0}")
        @ValueSource(ints = {0, 10081})
        void should_reject_values_out_of_bounds(int invalid) {
            // given
            final var base = validCheckDto();
            final CheckDto dto = new CheckDto(
                    1L, "n", "d", true, invalid,
                    base.nextRunAt(),
                    anyCheckType(),
                    validRestApiProfile(1L)
            );

            // when / then
            assertThat(validator.validate(dto))
                    .as("ожидаем нарушение для значения %s", invalid)
                    .isNotEmpty();
        }

        @ParameterizedTest(name = "[{index}] edge={0}")
        @ValueSource(ints = {1, 10080})
        void should_accept_edge_values_within_bounds(int edge) {
            // given
            final var base = validCheckDto();
            final CheckDto dto = new CheckDto(
                    1L, "n", "d", true, edge,
                    base.nextRunAt(),
                    anyCheckType(),
                    validRestApiProfile(1L)
            );

            // when / then
            assertThat(validator.validate(dto)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Обязательные поля (@NotNull)")
    class RequiredFieldsValidation {

        @Test
        void should_require_nextRunAt() {
            // given
            final CheckDto dto = new CheckDto(
                    1L, "n", "d", true, 5,
                    null,
                    anyCheckType(),
                    validRestApiProfile(1L)
            );

            // when
            final var violations = validator.validate(dto);

            // then
            assertHasViolationFor("nextRunAt", violations);
        }

        @Test
        void should_require_type() {
            // given
            final var base = validCheckDto();
            final CheckDto dto = new CheckDto(
                    1L, "n", "d", true, 5,
                    base.nextRunAt(),
                    null,
                    validRestApiProfile(1L)
            );

            // when
            final var violations = validator.validate(dto);

            // then
            assertHasViolationFor("type", violations);
        }

        @Test
        void should_require_profile() {
            // given
            final var base = validCheckDto();
            final CheckDto dto = new CheckDto(
                    1L, "n", "d", true, 5,
                    base.nextRunAt(),
                    anyCheckType(),
                    null
            );

            // when
            final var violations = validator.validate(dto);

            // then
            assertHasViolationFor("profile", violations);
        }
    }
}
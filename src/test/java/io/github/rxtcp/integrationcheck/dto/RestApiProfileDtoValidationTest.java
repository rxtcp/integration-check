package io.github.rxtcp.integrationcheck.dto;

import io.github.rxtcp.integrationcheck.enums.HttpMethod;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты валидации {@link RestApiProfileDto}.
 * <p>
 * Цели:
 * 1) Валидный профиль проходит без нарушений.
 * 2) Обязательные поля помечены как @NotNull/@NotBlank.
 * 3) Числовые поля валидируются по границам.
 * 4) Необязательные поля допускают null.
 */
@DisplayName("RestApiProfileDto: валидация bean-constraint'ов")
@DisplayNameGeneration(ReplaceUnderscores.class)
class RestApiProfileDtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setup_validator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    private static HttpMethod anyHttpMethod() {
        // Не завязываемся на конкретную константу enum
        return HttpMethod.values()[0];
    }

    private static RestApiProfileDto valid() {
        return new RestApiProfileDto(
                1L,
                10L,
                "https://example.org/health",
                anyHttpMethod(),
                5,
                "{\"X-Trace-Id\":[\"abc\"]}",
                null,
                200
        );
    }

    // ===== Позитивные сценарии =====

    /**
     * Нет нарушений валидации.
     */
    private static void assertNoViolations(RestApiProfileDto dto) {
        assertThat(validator.validate(dto)).isEmpty();
    }

    // ===== Обязательные поля =====

    /**
     * Среди нарушений есть нарушение для указанного поля.
     * Проверяем по имени propertyPath — стабильно и не зависит от локали.
     */
    private static void assertHasViolationFor(String field, Set<ConstraintViolation<RestApiProfileDto>> violations) {
        assertThat(violations)
                .as("ожидаем нарушение для поля '%s'", field)
                .anySatisfy(v -> assertThat(v.getPropertyPath().toString()).isEqualTo(field));
    }

    // ===== Числовые поля с границами =====

    @Nested
    @DisplayName("Позитивные сценарии")
    class PositiveCases {

        @Test
        void should_accept_valid_profile() {
            // given
            final var dto = valid();

            // then
            assertNoViolations(dto);
        }

        @Test
        void should_allow_null_headers_and_request_body() {
            // given
            final var dto = new RestApiProfileDto(
                    1L, 10L, "https://example.org", anyHttpMethod(), 5, null, null, 200
            );

            // then
            assertNoViolations(dto);
        }

        @Test
        void should_allow_null_profileId_on_create() {
            // given
            final var dto = new RestApiProfileDto(
                    1L, null, "https://example.org", anyHttpMethod(), 5, "{}", "{}", 200
            );

            // then
            assertNoViolations(dto);
        }
    }

    @Nested
    @DisplayName("Обязательные поля (@NotNull/@NotBlank)")
    class RequiredFields {

        @Test
        void should_require_checkId() {
            // given
            final var dto = new RestApiProfileDto(
                    null, 10L, "https://example.org", anyHttpMethod(), 5, null, null, 200
            );

            // when
            final var violations = validator.validate(dto);

            // then
            assertHasViolationFor("checkId", violations);
        }

        @Test
        void should_require_httpMethod() {
            // given
            final var dto = new RestApiProfileDto(
                    1L, 10L, "https://example.org", null, 5, null, null, 200
            );

            // when
            final var violations = validator.validate(dto);

            // then
            assertHasViolationFor("httpMethod", violations);
        }

        @ParameterizedTest(name = "[{index}] invalid url=''{0}''")
        @ValueSource(strings = {"   ", "not-a-url"})
        void should_require_non_blank_and_valid_url(String invalidUrl) {
            // given
            final var dto = new RestApiProfileDto(
                    1L, 10L, invalidUrl, anyHttpMethod(), 5, null, null, 200
            );

            // when
            final var violations = validator.validate(dto);

            // then
            assertHasViolationFor("url", violations);
        }
    }

    // ===== Вспомогательные методы =====

    @Nested
    @DisplayName("timeoutSeconds (@Min/@Max)")
    class TimeoutSecondsBounds {

        @ParameterizedTest(name = "[{index}] invalid={0}")
        @ValueSource(ints = {0, 601})
        void should_reject_values_outside_bounds(int invalid) {
            // given
            final var dto = new RestApiProfileDto(
                    1L, 10L, "https://example.org", anyHttpMethod(), invalid, null, null, 200
            );

            // then
            assertThat(validator.validate(dto)).isNotEmpty();
        }

        @ParameterizedTest(name = "[{index}] edge={0}")
        @ValueSource(ints = {1, 600})
        void should_accept_edge_values(int edge) {
            // given
            final var dto = new RestApiProfileDto(
                    1L, 10L, "https://example.org", anyHttpMethod(), edge, null, null, 200
            );

            // then
            assertNoViolations(dto);
        }
    }

    @Nested
    @DisplayName("expectedHttpCode (@Min/@Max)")
    class ExpectedHttpCodeBounds {

        @ParameterizedTest(name = "[{index}] invalid={0}")
        @ValueSource(ints = {99, 600})
        void should_reject_values_outside_bounds(int invalid) {
            // given
            final var dto = new RestApiProfileDto(
                    1L, 10L, "https://example.org", anyHttpMethod(), 5, null, null, invalid
            );

            // then
            assertThat(validator.validate(dto)).isNotEmpty();
        }

        @ParameterizedTest(name = "[{index}] edge={0}")
        @ValueSource(ints = {100, 599})
        void should_accept_edge_values(int edge) {
            // given
            final var dto = new RestApiProfileDto(
                    1L, 10L, "https://example.org", anyHttpMethod(), 5, null, null, edge
            );

            // then
            assertNoViolations(dto);
        }
    }
}
// src/test/java/io/github/rxtcp/integrationcheck/entity/RestApiProfileEntityValidationTest.java
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
import static io.github.rxtcp.integrationcheck.entity.EntityTestFixtures.newRestProfile;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RestApiProfile: валидация сущности")
@DisplayNameGeneration(ReplaceUnderscores.class)
class RestApiProfileEntityValidationTest {

    private static final int TIMEOUT_MIN = 1;
    private static final int TIMEOUT_MAX = 600;
    private static final int HTTP_CODE_MIN = 100;
    private static final int HTTP_CODE_MAX = 599;

    private static Validator validator;

    @BeforeAll
    static void setup_validator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    /**
     * Вспомогательная фабрика: профиль с установленным владельцем (поле {@code check} обязательно).
     */
    private static RestApiProfile profileWithOwner() {
        final RestApiProfile profile = newRestProfile();
        profile.setCheck(newCheck("owner"));
        return profile;
    }

    private static void assertNoViolations(RestApiProfile entity) {
        assertThat(validator.validate(entity)).isEmpty();
    }

    /**
     * Проверяем по имени propertyPath — стабильно и не зависит от локали/текстов сообщений.
     */
    private static void assertHasViolationFor(String field, Set<ConstraintViolation<RestApiProfile>> violations) {
        assertThat(violations)
                .as("ожидаем нарушение для поля '%s'", field)
                .anySatisfy(v -> assertThat(v.getPropertyPath().toString()).isEqualTo(field));
    }

    @Nested
    @DisplayName("Позитивные сценарии")
    class PositiveCases {

        @Test
        void should_have_no_violations_for_valid_profile() {
            final RestApiProfile profile = profileWithOwner();

            assertNoViolations(profile);
        }
    }

    @Nested
    @DisplayName("Обязательные поля")
    class RequiredFields {

        @Test
        void should_require_check_owner() {
            final RestApiProfile profile = newRestProfile(); // без owner

            final var violations = validator.validate(profile);

            assertHasViolationFor("check", violations);
        }
    }

    @Nested
    @DisplayName("Поле url (@NotBlank)")
    class UrlValidation {

        @ParameterizedTest(name = "[{index}] blank=''{0}''")
        @ValueSource(strings = {"  ", "\t", "\n"})
        void should_reject_blank_url(String blankUrl) {
            final RestApiProfile profile = profileWithOwner();
            profile.setUrl(blankUrl);

            final var violations = validator.validate(profile);

            assertHasViolationFor("url", violations);
        }
    }

    // ===== Утилиты для лаконичных и стабильных проверок =====

    @Nested
    @DisplayName("Поле timeoutSeconds (@Min/@Max)")
    class TimeoutBounds {

        @ParameterizedTest(name = "[{index}] invalid={0}")
        @ValueSource(ints = {0, TIMEOUT_MAX + 1})
        void should_reject_values_out_of_bounds(int invalid) {
            final RestApiProfile profile = profileWithOwner();
            profile.setTimeoutSeconds(invalid);

            assertThat(validator.validate(profile)).isNotEmpty();
        }

        @ParameterizedTest(name = "[{index}] edge={0}")
        @ValueSource(ints = {TIMEOUT_MIN, TIMEOUT_MAX})
        void should_accept_edge_values(int edge) {
            final RestApiProfile profile = profileWithOwner();
            profile.setTimeoutSeconds(edge);

            assertNoViolations(profile);
        }
    }

    @Nested
    @DisplayName("Поле expectedHttpCode (@Min/@Max)")
    class ExpectedHttpCodeBounds {

        @ParameterizedTest(name = "[{index}] invalid={0}")
        @ValueSource(ints = {HTTP_CODE_MIN - 1, HTTP_CODE_MAX + 1})
        void should_reject_values_out_of_bounds(int invalid) {
            final RestApiProfile profile = profileWithOwner();
            profile.setExpectedHttpCode(invalid);

            assertThat(validator.validate(profile)).isNotEmpty();
        }

        @ParameterizedTest(name = "[{index}] edge={0}")
        @ValueSource(ints = {HTTP_CODE_MIN, HTTP_CODE_MAX})
        void should_accept_edge_values(int edge) {
            final RestApiProfile profile = profileWithOwner();
            profile.setExpectedHttpCode(edge);

            assertNoViolations(profile);
        }
    }
}
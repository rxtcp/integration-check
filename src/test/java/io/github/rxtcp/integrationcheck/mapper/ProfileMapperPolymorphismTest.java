package io.github.rxtcp.integrationcheck.mapper;

import io.github.rxtcp.integrationcheck.dto.CheckProfileDto;
import io.github.rxtcp.integrationcheck.dto.RestApiProfileDto;
import io.github.rxtcp.integrationcheck.entity.Check;
import io.github.rxtcp.integrationcheck.entity.CheckProfile;
import io.github.rxtcp.integrationcheck.entity.RestApiProfile;
import io.github.rxtcp.integrationcheck.domain.HttpMethod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static io.github.rxtcp.integrationcheck.mapper.MapperFixtures.newCheck;
import static io.github.rxtcp.integrationcheck.mapper.MapperFixtures.newRestProfile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@SpringBootTest(classes = MapperTestConfig.class)
@DisplayName("ProfileMapper: полиморфный маппинг профилей")
@DisplayNameGeneration(ReplaceUnderscores.class)
class ProfileMapperPolymorphismTest {

    @Autowired
    private ProfileMapper mapper;

    // ===== Небольшие фабрики, чтобы не дублировать сеттеры в тестах =====
    private static RestApiProfile rp(
            Long id,
            Check owner,
            String url,
            HttpMethod method,
            Integer timeoutSec,
            String headersJson,
            String requestBody,
            Integer expectedCode
    ) {
        final RestApiProfile p = new RestApiProfile();
        p.setId(id);
        p.setCheck(owner);
        p.setUrl(url);
        p.setHttpMethod(method);
        if (timeoutSec != null) p.setTimeoutSeconds(timeoutSec);
        p.setHeaders(headersJson);
        p.setRequestBody(requestBody);
        if (expectedCode != null) p.setExpectedHttpCode(expectedCode);
        return p;
    }

    /**
     * Неподдерживаемый подсабкласс для fast-fail-проверки.
     */
    static class UnknownProfile extends CheckProfile { /* пусто */
    }

    @Nested
    @DisplayName("Перегрузка toDto(RestApiProfile)")
    class RestApiProfileOverload {

        @Test
        void should_return_null_when_source_is_null() {
            assertThat(mapper.toDto((RestApiProfile) null)).isNull();
        }

        @Test
        void should_map_all_fields_via_restApiProfileMapper() {
            final Check check = newCheck(10L, "n");
            final RestApiProfile profile = rp(
                    20L, check,
                    "https://example.org/health",
                    HttpMethod.GET,
                    15,
                    "{\"X-Trace-Id\":[\"abc\"]}",
                    "body",
                    200
            );

            final RestApiProfileDto dto = mapper.toDto(profile);

            assertThat(dto.checkId()).isEqualTo(10L);
            assertThat(dto.profileId()).isEqualTo(20L);
            assertThat(dto.url()).isEqualTo("https://example.org/health");
            assertThat(dto.httpMethod()).isEqualTo(HttpMethod.GET);
            assertThat(dto.timeoutSeconds()).isEqualTo(15);
            assertThat(dto.headers()).isEqualTo("{\"X-Trace-Id\":[\"abc\"]}");
            assertThat(dto.requestBody()).isEqualTo("body");
            assertThat(dto.expectedHttpCode()).isEqualTo(200);
        }

        @Test
        void should_throw_NPE_when_check_owner_is_not_initialized() {
            final RestApiProfile profile = rp(
                    30L, null,
                    "https://x",
                    HttpMethod.GET,
                    5,
                    null,
                    null,
                    200
            );

            assertThatThrownBy(() -> mapper.toDto(profile))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void should_map_null_profile_id() {
            final Check check = newCheck(1L, "n");
            final RestApiProfile profile = newRestProfile(null, check);
            profile.setUrl("https://u");
            profile.setHttpMethod(HttpMethod.POST);
            profile.setTimeoutSeconds(7);
            profile.setExpectedHttpCode(201);

            final RestApiProfileDto dto = mapper.toDto(profile);

            assertThat(dto.checkId()).isEqualTo(1L);
            assertThat(dto.profileId()).isNull();
            assertThat(dto.url()).isEqualTo("https://u");
            assertThat(dto.httpMethod()).isEqualTo(HttpMethod.POST);
            assertThat(dto.timeoutSeconds()).isEqualTo(7);
            assertThat(dto.expectedHttpCode()).isEqualTo(201);
        }
    }

    @Nested
    @DisplayName("Полиморфизм: toDto(CheckProfile)")
    class BaseTypeDispatch {

        @Test
        void should_dispatch_to_restApiProfile_and_map_all_fields() {
            final Check check = newCheck(1L, "n");
            final RestApiProfile profile = rp(
                    2L, check,
                    "https://example.org/ok",
                    HttpMethod.PUT,
                    9,
                    "{\"k\":[\"v\"]}",
                    "{\"x\":1}",
                    204
            );

            final CheckProfileDto dto = mapper.toDto((CheckProfile) profile);

            assertThat(dto)
                    .as("ожидаем конкретный RestApiProfileDto как реализацию sealed-типа")
                    .isInstanceOfSatisfying(RestApiProfileDto.class, rest -> {
                        assertThat(rest.checkId()).isEqualTo(1L);
                        assertThat(rest.profileId()).isEqualTo(2L);
                        assertThat(rest.url()).isEqualTo("https://example.org/ok");
                        assertThat(rest.httpMethod()).isEqualTo(HttpMethod.PUT);
                        assertThat(rest.timeoutSeconds()).isEqualTo(9);
                        assertThat(rest.headers()).isEqualTo("{\"k\":[\"v\"]}");
                        assertThat(rest.requestBody()).isEqualTo("{\"x\":1}");
                        assertThat(rest.expectedHttpCode()).isEqualTo(204);
                    });
        }

        @Test
        void should_fail_fast_for_unknown_subtype() {
            final UnknownProfile unknown = new UnknownProfile();

            assertThatThrownBy(() -> mapper.toDto(unknown))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Неподдерживаемый тип профиля");
        }
    }
}
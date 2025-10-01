package io.github.rxtcp.integrationcheck.mapper;

import io.github.rxtcp.integrationcheck.dto.RestApiProfileDto;
import io.github.rxtcp.integrationcheck.entity.Check;
import io.github.rxtcp.integrationcheck.entity.RestApiProfile;
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
@DisplayName("RestApiProfileMapper: маппинг RestApiProfile → RestApiProfileDto")
@DisplayNameGeneration(ReplaceUnderscores.class)
class RestApiProfileMapperTest {

    private static final long CHECK_ID = 5L;
    private static final String CHECK_NAME = "api";
    private static final long PROFILE_ID = 7L;

    // Ожидаемые значения из фикстур (см. MapperFixtures)
    private static final String EXPECTED_URL = "https://example.org/health";
    private static final int EXPECTED_TIMEOUT = 30;
    private static final int EXPECTED_HTTP_CODE = 200;

    @Autowired
    private RestApiProfileMapper mapper;

    @Nested
    @DisplayName("Позитивные сценарии")
    class PositiveCases {

        @Test
        void should_map_all_fields() {
            // given: валидная сущность с владельцем
            final Check check = newCheck(CHECK_ID, CHECK_NAME);
            final RestApiProfile profile = newRestProfile(PROFILE_ID, check);

            // when
            final RestApiProfileDto dto = mapper.toDto(profile);

            // then: все значимые поля перенесены корректно
            assertThat(dto.checkId()).isEqualTo(CHECK_ID);
            assertThat(dto.profileId()).isEqualTo(PROFILE_ID);
            assertThat(dto.url()).isEqualTo(EXPECTED_URL);
            // метод не фиксируем «магически»: сравниваем с исходным профилем
            assertThat(dto.httpMethod()).isEqualTo(profile.getHttpMethod());
            assertThat(dto.timeoutSeconds()).isEqualTo(EXPECTED_TIMEOUT);
            // заголовки переносим «как есть» — достаточно маркера ключа
            assertThat(dto.headers()).contains("X-Trace-Id");
            // тело запроса в фикстуре отсутствует
            assertThat(dto.requestBody()).isNull();
            assertThat(dto.expectedHttpCode()).isEqualTo(EXPECTED_HTTP_CODE);
        }
    }

    @Nested
    @DisplayName("Негативные сценарии")
    class NegativeCases {

        @Test
        void should_throw_NPE_when_owner_check_is_not_initialized() {
            // given: профиль без владельца — текущая реализация nested-mapper ожидает не-null
            final RestApiProfile profile = newRestProfile(1L, null);

            // then
            assertThatThrownBy(() -> mapper.toDto(profile))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}
package io.github.rxtcp.integrationcheck.mapper;

import io.github.rxtcp.integrationcheck.dto.CheckDto;
import io.github.rxtcp.integrationcheck.dto.RestApiProfileDto;
import io.github.rxtcp.integrationcheck.entity.Check;
import io.github.rxtcp.integrationcheck.entity.RestApiProfile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
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
@DisplayName("CheckMapper: маппинг Check → CheckDto")
@DisplayNameGeneration(ReplaceUnderscores.class)
class CheckMapperTest {

    private static final long CHECK_ID = 42L;
    private static final String CHECK_NAME = "health";
    private static final long PROFILE_ID = 77L;

    @Autowired
    private CheckMapper mapper;

    @Test
    void should_map_scalar_fields_and_nested_profile() {
        // given
        final Check check = newCheck(CHECK_ID, CHECK_NAME);
        final RestApiProfile profile = newRestProfile(PROFILE_ID, check);
        check.setProfile(profile);

        // when
        final CheckDto dto = mapper.toDto(check);

        // then: скалярные поля
        assertThat(dto.id()).isEqualTo(CHECK_ID);
        assertThat(dto.name()).isEqualTo(CHECK_NAME);
        assertThat(dto.description()).isEqualTo("desc"); // фикстура задаёт "desc"
        assertThat(dto.enabled()).isTrue();
        assertThat(dto.runIntervalMin()).isEqualTo(5);
        assertThat(dto.nextRunAt()).isEqualTo(check.getNextRunAt());
        assertThat(dto.type()).isEqualTo(check.getType());

        // then: вложенный профиль — проверяем тип и значения без ручного кастинга
        assertThat(dto.profile())
                .as("ожидаем RestApiProfileDto как реализацию sealed-интерфейса профиля")
                .isInstanceOfSatisfying(RestApiProfileDto.class, p -> {
                    assertThat(p.checkId()).isEqualTo(CHECK_ID);
                    assertThat(p.profileId()).isEqualTo(PROFILE_ID);
                });
    }

    @Test
    void should_set_null_profile_when_entity_profile_is_null() {
        // given
        final Check check = newCheck(1L, "no-profile");
        check.setProfile(null);

        // when
        final CheckDto dto = mapper.toDto(check);

        // then
        assertThat(dto.profile()).isNull();
    }

    @Test
    void should_throw_NPE_when_nested_profile_has_no_owner_check() {
        // given: сломанная модель — профиль без ссылки на владельца
        final Check check = newCheck(2L, "broken");
        final RestApiProfile profileWithoutOwner = newRestProfile(3L, null);
        check.setProfile(profileWithoutOwner);

        // then: поведение текущего nested-mapper — NPE (фиксируем контракт)
        assertThatThrownBy(() -> mapper.toDto(check))
                .isInstanceOf(NullPointerException.class);
    }
}
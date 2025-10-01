package io.github.rxtcp.integrationcheck.mapper;

import io.github.rxtcp.integrationcheck.dto.CheckDto;
import io.github.rxtcp.integrationcheck.dto.RestApiProfileDto;
import io.github.rxtcp.integrationcheck.entity.Check;
import io.github.rxtcp.integrationcheck.entity.RestApiProfile;
import io.github.rxtcp.integrationcheck.domain.HttpMethod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static io.github.rxtcp.integrationcheck.mapper.MapperFixtures.newCheck;
import static io.github.rxtcp.integrationcheck.mapper.MapperFixtures.newRestProfile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;

@ActiveProfiles("test")
@SpringBootTest(classes = MapperTestConfig.class)
@DisplayName("CheckMapper → делегирование маппинга профиля в ProfileMapper")
@DisplayNameGeneration(ReplaceUnderscores.class)
class CheckMapperDelegationMockTest {

    private static final long CHECK_ID = 10L;
    private static final long PROFILE_ID = 20L;

    @Autowired
    private CheckMapper checkMapper;

    // SpyBean оставляем — хотим настоящий контекст и поведение маппера, но перехватываем делегирование
    @MockitoSpyBean
    private ProfileMapper profileMapper;

    @Test
    void should_delegate_profile_mapping_to_ProfileMapper() {
        // given
        final Check check = newCheck(CHECK_ID, "with-profile");
        final RestApiProfile profile = newRestProfile(PROFILE_ID, check);
        check.setProfile(profile);

        final RestApiProfileDto stub = new RestApiProfileDto(
                CHECK_ID,
                PROFILE_ID,
                "https://stub",
                HttpMethod.GET, // конкретный метод не важен — фиксируем для читаемости
                10,
                null,
                null,
                201
        );

        // Важно: для spy используем doReturn(...).when(...) — не даём вызваться реальной реализации
        doReturn(stub).when(profileMapper).toDto(any(RestApiProfile.class));

        // when
        final CheckDto dto = checkMapper.toDto(check);

        // then
        final ArgumentCaptor<RestApiProfile> captor = ArgumentCaptor.forClass(RestApiProfile.class);
        then(profileMapper).should(times(1)).toDto(captor.capture());

        // пробрасывался ровно тот профиль, который прикреплён к чек-сущности
        final RestApiProfile delegated = captor.getValue();
        assertThat(delegated.getId()).isEqualTo(PROFILE_ID);
        assertThat(delegated.getCheck().getId()).isEqualTo(CHECK_ID);

        // в результате в DTO использован именно возвращённый заглушкой объект (идентичность, не только equals)
        assertThat(dto.profile()).isSameAs(stub);
    }
}
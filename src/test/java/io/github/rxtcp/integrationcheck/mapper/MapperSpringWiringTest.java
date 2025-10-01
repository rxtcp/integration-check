package io.github.rxtcp.integrationcheck.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(classes = MapperTestConfig.class)
@DisplayName("Spring wiring для MapStruct-мэпперов")
@DisplayNameGeneration(ReplaceUnderscores.class)
class MapperSpringWiringTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private CheckMapper checkMapper;
    @Autowired
    private ProfileMapper profileMapper;
    @Autowired
    private RestApiProfileMapper restApiProfileMapper;

    @Test
    void should_expose_required_mappers_as_single_spring_beans() {
        // Проверяем, что по одному бину каждого типа присутствует в контексте
        assertThat(applicationContext.getBeansOfType(CheckMapper.class)).hasSize(1);
        assertThat(applicationContext.getBeansOfType(ProfileMapper.class)).hasSize(1);
        assertThat(applicationContext.getBeansOfType(RestApiProfileMapper.class)).hasSize(1);

        // Проверяем, что внедрённые зависимости — те же экземпляры, что в контексте
        assertThat(applicationContext.getBean(CheckMapper.class)).isSameAs(checkMapper);
        assertThat(applicationContext.getBean(ProfileMapper.class)).isSameAs(profileMapper);
        assertThat(applicationContext.getBean(RestApiProfileMapper.class)).isSameAs(restApiProfileMapper);
    }
}
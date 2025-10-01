package io.github.rxtcp.integrationcheck;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(
        classes = IntegrationCheckApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@DisplayName("IntegrationCheckApplication — контекст и инфраструктура планирования")
@DisplayNameGeneration(ReplaceUnderscores.class)
class IntegrationCheckApplicationContextTest {

    private final ApplicationContext context;

    @Autowired
    IntegrationCheckApplicationContextTest(ApplicationContext context) {
        this.context = context;
    }

    @Test
    @DisplayName("должен поднять контекст и зарегистрировать инфраструктуру @Scheduled")
    void should_load_context_and_enable_scheduling_infrastructure() {
        // контекст поднялся
        assertThat(context).isNotNull();

        // @EnableScheduling → наличие пост-процессора, обрабатывающего @Scheduled
        ScheduledAnnotationBeanPostProcessor sabpp =
                context.getBean(ScheduledAnnotationBeanPostProcessor.class);
        assertThat(sabpp).as("ScheduledAnnotationBeanPostProcessor должен быть зарегистрирован").isNotNull();

        // Автоконфигурация планировщика доступна (обычно ThreadPoolTaskScheduler)
        Map<String, TaskScheduler> schedulers = context.getBeansOfType(TaskScheduler.class);
        assertThat(schedulers)
                .as("в контексте должен быть хотя бы один TaskScheduler")
                .isNotEmpty();
    }
}
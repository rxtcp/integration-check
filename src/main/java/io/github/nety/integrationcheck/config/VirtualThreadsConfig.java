package io.github.nety.integrationcheck.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

@Configuration
public class VirtualThreadsConfig {

    @Bean
    public TaskExecutor virtualLimitedTaskExecutor() {
        SimpleAsyncTaskExecutor exec = new SimpleAsyncTaskExecutor(
                Thread.ofVirtual()
                        .name("vt-batch-", 0)
                        .factory()
        );
        exec.setConcurrencyLimit(20);

        return exec;
    }
}

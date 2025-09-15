package io.github.nety.integrationcheck.config;

import io.github.nety.integrationcheck.config.properties.JobLauncherProperties;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

@Configuration
public class JobLauncherConfig {

    @Bean
    @Primary
    public JobLauncher asyncJobLauncher(JobRepository jobRepository, TaskExecutor jobLauncherExecutor) throws Exception {
        var jobLauncher = new TaskExecutorJobLauncher();
        jobLauncher.setJobRepository(jobRepository);
        jobLauncher.setTaskExecutor(jobLauncherExecutor);
        jobLauncher.afterPropertiesSet();
        return jobLauncher;
    }

    @Bean
    public TaskExecutor jobLauncherExecutor(JobLauncherProperties properties) {
        var executor = new SimpleAsyncTaskExecutor(properties.threadNamePrefix());
        executor.setVirtualThreads(true);
        executor.setConcurrencyLimit(properties.concurrencyLimit());
        return executor;
    }
}

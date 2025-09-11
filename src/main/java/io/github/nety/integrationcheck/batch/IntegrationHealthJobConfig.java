package io.github.nety.integrationcheck.batch;

import io.github.nety.integrationcheck.batch.adapter.ReaderAdapters;
import io.github.nety.integrationcheck.domain.Check;
import io.github.nety.integrationcheck.domain.CheckResult;
import io.github.nety.integrationcheck.service.CheckProcessor;
import io.github.nety.integrationcheck.service.CheckReader;
import io.github.nety.integrationcheck.service.CheckResultWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.*;
import org.springframework.batch.item.adapter.ItemProcessorAdapter;
import org.springframework.batch.item.adapter.ItemWriterAdapter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.VirtualThreadTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class IntegrationHealthJobConfig {

    private final CheckReader checkReader;
    private final CheckProcessor checkProcessor;
    private final CheckResultWriter checkResultWriter;

    @Bean
    @org.springframework.batch.core.configuration.annotation.StepScope
    public ItemStreamReader<Check> checksReader() {
        List<Check> checks = checkReader.readChecks();
        ListItemReader<Check> listReader = new ListItemReader<>(checks);

        SynchronizedItemStreamReader<Check> synced = new SynchronizedItemStreamReader<>();
        synced.setDelegate(ReaderAdapters.asNoopItemStreamReader(listReader));
        return synced;
    }

    @Bean
    public ItemProcessor<Check, CheckResult> checksProcessor() {
        ItemProcessorAdapter<Check, CheckResult> adapter = new ItemProcessorAdapter<>();
        adapter.setTargetObject(checkProcessor);
        adapter.setTargetMethod("process");
        return adapter;
    }

    @Bean
    public ItemWriter<CheckResult> checksWriter() {
        ItemWriterAdapter<CheckResult> adapter = new ItemWriterAdapter<>();
        adapter.setTargetObject(checkResultWriter);
        adapter.setTargetMethod("saveResult");
        return adapter;
    }

    /** Виртуальные потоки: "одна задача = один поток" */
    @Bean
    public TaskExecutor checksTaskExecutor() {
        return new VirtualThreadTaskExecutor("checks-");
    }

//    /**
//     * Пул потоков для параллелизма шага.
//     */
//    @Bean
//    public TaskExecutor checksTaskExecutor() {
//        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
//        exec.setThreadNamePrefix("checks-");
//        exec.setCorePoolSize(8);
//        exec.setMaxPoolSize(8);
//        exec.setQueueCapacity(0); // direct handoff для ровного распределения
//        exec.initialize();
//        return exec;
//    }

    @Bean
    public Step checksStep(JobRepository jobRepository,
                           PlatformTransactionManager transactionManager,
                           ItemStreamReader<Check> checksReader,
                           ItemProcessor<Check, CheckResult> checksProcessor,
                           ItemWriter<CheckResult> checksWriter,
                           TaskExecutor checksTaskExecutor) {

        return new StepBuilder("checksStep", jobRepository)
                .<Check, CheckResult>chunk(1, transactionManager)  // 1 чек = 1 транзакция
                .reader(checksReader)
                .processor(checksProcessor)
                .writer(checksWriter)
                .taskExecutor(checksTaskExecutor)                  // включает параллельность
                .build();
    }

    @Bean
    public Job integrationHealthJob(JobRepository jobRepository, Step checksStep) {
        return new JobBuilder("integrationHealthJob", jobRepository)
                .start(checksStep)
                .build();
    }
}
package io.github.nety.integrationcheck.config;

import io.github.nety.integrationcheck.config.properties.IntegrationHealthCheckJobProperties;
import io.github.nety.integrationcheck.domain.Check;
import io.github.nety.integrationcheck.domain.CheckResult;
import io.github.nety.integrationcheck.service.CheckProcessor;
import io.github.nety.integrationcheck.service.CheckReader;
import io.github.nety.integrationcheck.service.CheckResultWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.HashMap;
import java.util.List;

@RequiredArgsConstructor
@Configuration
public class IntegrationHealthCheckJobConfig {

    private static final String JOB_NAME = "integrationHealthCheckJob";
    private static final String MASTER_STEP = "masterStep";
    private static final String WORKER_STEP = "workerStep";
    private static final String PARTITION_PREFIX = "check-";

    @Bean
    public Job integrationHealthCheckJob(Step masterStep, JobRepository jobRepository) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(masterStep)
                .build();
    }

    @Bean
    public Step masterStep(Step workerStep, Partitioner listRangePartitioner, TaskExecutor partitionExecutor,
                           JobRepository jobRepository) {
        return new StepBuilder(MASTER_STEP, jobRepository)
                .partitioner(WORKER_STEP, listRangePartitioner)
                .step(workerStep)
                .taskExecutor(partitionExecutor)
                .build();
    }

    @Bean
    public Step workerStep(IntegrationHealthCheckJobProperties properties,
                           JobRepository jobRepository, PlatformTransactionManager transactionManager,
                           ItemReader<Check> reader, ItemProcessor<Check, CheckResult> processor,
                           ItemWriter<CheckResult> writer) {
        return new StepBuilder(WORKER_STEP, jobRepository)
                .<Check, CheckResult>chunk(properties.chunkSize(), transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }

    @Bean
    public TaskExecutor partitionExecutor(IntegrationHealthCheckJobProperties properties) {
        var executor = new SimpleAsyncTaskExecutor(properties.threadNamePrefix());
        executor.setVirtualThreads(true);
        executor.setConcurrencyLimit(properties.concurrencyLimit());
        return executor;
    }

    @Bean
    public Partitioner checkIdPartitioner(CheckReader checkReader) {
        return gridSize -> {
            var partitionContexts = new HashMap<String, ExecutionContext>();
            checkReader.findDueCheckIds().forEach(checkId -> {
                var executionContext = new ExecutionContext();
                executionContext.putLong("checkId", checkId);
                partitionContexts.put(PARTITION_PREFIX + checkId, executionContext);
            });
            return partitionContexts;
        };
    }

    @Bean
    @StepScope
    public ItemReader<Check> reader(
            @Value("#{stepExecutionContext['checkId']}") Long checkId,
            CheckReader checkReader
    ) {
        return new ListItemReader<>(List.of(
                checkReader.findCheckById(checkId)
        ));
    }

    @Bean
    public ItemProcessor<Check, CheckResult> processor(CheckProcessor checkProcessor) {
        return checkProcessor::process;
    }

    @Bean
    public ItemWriter<CheckResult> writer(CheckResultWriter checkResultWriter) {
        return checksResults -> {
            for (var result : checksResults) {
                checkResultWriter.saveResult(result);
            }
        };
    }
}


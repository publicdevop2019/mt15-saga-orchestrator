package com.hw.config;

import com.hw.aggregate.sm.command.CreateBizStateMachineCommand;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;

@Configuration
@EnableBatchProcessing
public class BatchConfig {
    @Autowired
    public JobBuilderFactory jobBuilderFactory;

    @Autowired
    public StepBuilderFactory stepBuilderFactory;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ExpireOrderItemReader expireOrderItemReader;

    @Autowired
    private ExpireOrderItemWriter expireOrderItemWriter;

    @Bean
    public ExpireOrderProcessor processor() {
        return new ExpireOrderProcessor();
    }

    @Autowired
    @Qualifier("CustomPool")
    private TaskExecutor customExecutor;
//    @Bean
//    public AsyncItemProcessor<CreateBizStateMachineCommand, CreateBizStateMachineCommand> asyncProcessor() {
//        AsyncItemProcessor<CreateBizStateMachineCommand, CreateBizStateMachineCommand> asyncItemProcessor = new AsyncItemProcessor<>();
//        asyncItemProcessor.setDelegate(itemProcessor());
//        asyncItemProcessor.setTaskExecutor(taskExecutor());
//
//        return asyncItemProcessor;
//    }

    @Bean
    public Job releaseExpireOrder(JobCompletionNotificationListener listener, Step step1) {
        return jobBuilderFactory.get("releaseExpireOrder")
                .listener(listener)
                .flow(step1)
                .end()
                .build();
    }

    @Bean
    public Step release() {
        return stepBuilderFactory.get("step1")
                .<CreateBizStateMachineCommand, CreateBizStateMachineCommand>chunk(10)
                .reader(expireOrderItemReader)
                .processor(processor())
                .writer(expireOrderItemWriter)
                .build();
    }

    @Bean
    public BatchConfigurer batchConfigurer() {
        return new DefaultBatchConfigurer() {
            @Override
            protected JobLauncher createJobLauncher() throws Exception {
                SimpleJobLauncher jobLauncher = new SimpleJobLauncher();
                jobLauncher.setJobRepository(jobRepository);
                jobLauncher.setTaskExecutor(customExecutor);
                return jobLauncher;
            }
        };
    }

    @Bean
    ReleaseJobContext getCommands() {
        return new ReleaseJobContext();
    }
}

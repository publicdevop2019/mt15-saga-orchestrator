package com.hw.config.batch;

import com.hw.aggregate.sm.command.CreateBizStateMachineCommand;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.integration.async.AsyncItemProcessor;
import org.springframework.batch.integration.async.AsyncItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.persistence.EntityManagerFactory;
import java.util.concurrent.Future;

@Configuration
//@EnableBatchProcessing
public class BatchConfig {
//    @Autowired
//    public JobBuilderFactory jobBuilderFactory;
//
//    @Autowired
//    public StepBuilderFactory stepBuilderFactory;
//
//    @Autowired
//    private JobRepository jobRepository;
//
//    @Autowired
//    private ExpireOrderItemReader expireOrderItemReader;
//
//    @Autowired
//    private ExpireOrderItemWriter expireOrderItemWriter;
//
//    @Autowired
//    @Qualifier("CustomPool")
//    private TaskExecutor customExecutor;
//
//    @Autowired
//    JobLauncher jobLauncher;
//    @Autowired
//    EntityManagerFactory entityManagerFactory;
//    @Autowired
//    ExpireOrderProcessor expireOrderProcessor;
//
//    @Bean
//    public Job releaseExpireOrder(JobCompletionNotificationListener listener, Step step1) {
//        return jobBuilderFactory.get("releaseExpireOrder")
//                .listener(listener)
//                .flow(step1)
//                .end()
//                .build();
//    }
//
//    @Bean
//    public AsyncItemWriter<CreateBizStateMachineCommand> asyncWriter() {
//        AsyncItemWriter<CreateBizStateMachineCommand> asyncItemWriter = new AsyncItemWriter<>();
//        asyncItemWriter.setDelegate(expireOrderItemWriter);
//        return asyncItemWriter;
//    }
//
//    @Bean
//    public AsyncItemProcessor<CreateBizStateMachineCommand, CreateBizStateMachineCommand> asyncProcessor() {
//        AsyncItemProcessor<CreateBizStateMachineCommand, CreateBizStateMachineCommand> asyncItemProcessor = new AsyncItemProcessor<>();
//        asyncItemProcessor.setDelegate(expireOrderProcessor);
//        asyncItemProcessor.setTaskExecutor(customExecutor);
//
//        return asyncItemProcessor;
//    }
//
//    @Bean
//    public Step release() {
//        return stepBuilderFactory.get("release")
//                .<CreateBizStateMachineCommand, Future<CreateBizStateMachineCommand>>chunk(10)
//                .reader(expireOrderItemReader)
//                .processor(asyncProcessor())
//                .writer(asyncWriter())
//                .build();
//    }
//
//    @Bean
//    public BatchConfigurer batchConfigurer() {
//        return new DefaultBatchConfigurer() {
//            @Override
//            public PlatformTransactionManager getTransactionManager() {
//                return new JpaTransactionManager(entityManagerFactory);// use this otherwise async will not work -> no transaction enabled
//            }
//
//            @Override
//            protected JobLauncher createJobLauncher() throws Exception {
//                SimpleJobLauncher jobLauncher = new SimpleJobLauncher();
//                jobLauncher.setJobRepository(jobRepository);
//                jobLauncher.setTaskExecutor(customExecutor);
//                jobLauncher.afterPropertiesSet();
//                return jobLauncher;
//            }
//        };
//    }

    @Bean
    ReleaseJobContext getCommands() {
        return new ReleaseJobContext();
    }
}

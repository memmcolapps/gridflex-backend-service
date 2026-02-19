package com.hes.datacollection.config;

import org.quartz.spi.JobFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * Quartz Scheduler configuration.
 *
 * Uses JDBCJobStore backed by the existing qrtz_* tables already in the DB.
 * Jobs are auto-wired with Spring beans via SpringBeanJobFactory.
 */
@Configuration
public class QuartzConfig {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private DataSource dataSource;

    @Bean
    public JobFactory jobFactory() {
        SpringBeanJobFactory factory = new SpringBeanJobFactory();
        factory.setApplicationContext(applicationContext);
        return factory;
    }

    @Bean
    public SchedulerFactoryBean schedulerFactoryBean(JobFactory jobFactory) {
        SchedulerFactoryBean factory = new SchedulerFactoryBean();

        factory.setJobFactory(jobFactory);
        factory.setDataSource(dataSource);
        factory.setOverwriteExistingJobs(true);
        factory.setWaitForJobsToCompleteOnShutdown(true);
        factory.setAutoStartup(true);

        factory.setQuartzProperties(quartzProperties());

        return factory;
    }

    private Properties quartzProperties() {
        Properties props = new Properties();

        // Scheduler identity
        props.setProperty("org.quartz.scheduler.instanceName",           "HESDataCollectionScheduler");
        props.setProperty("org.quartz.scheduler.instanceId",             "AUTO");

        // Thread pool
        props.setProperty("org.quartz.threadPool.class",                 "org.quartz.simpl.SimpleThreadPool");
        props.setProperty("org.quartz.threadPool.threadCount",           "10");
        props.setProperty("org.quartz.threadPool.threadPriority",        "5");

        // JobStore — PostgreSQL JDBC store backed by the existing qrtz_* tables
        props.setProperty("org.quartz.jobStore.class",
                "org.quartz.impl.jdbcjobstore.JobStoreTX");
        props.setProperty("org.quartz.jobStore.driverDelegateClass",
                "org.quartz.impl.jdbcjobstore.PostgreSQLDelegate");
        props.setProperty("org.quartz.jobStore.useProperties",           "false");
        props.setProperty("org.quartz.jobStore.tablePrefix",             "QRTZ_");
        props.setProperty("org.quartz.jobStore.isClustered",             "false");
        props.setProperty("org.quartz.jobStore.misfireThreshold",        "60000");

        // Serialization
        props.setProperty("org.quartz.jobStore.dataSource",              "quartzDS");

        return props;
    }
}

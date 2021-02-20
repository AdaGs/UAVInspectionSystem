package com.gjdw.stserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@EnableAsync
public class SchedulingConfig {

    @Bean
     public TaskScheduler taskScheduler() {
         ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
         // 定时任务执行线程池核心线程数
         taskScheduler.setPoolSize(15);
         taskScheduler.setRemoveOnCancelPolicy(true);
         taskScheduler.setThreadNamePrefix("TaskSchedulerThreadPool-");
         taskScheduler.setAwaitTerminationSeconds(60);
         return taskScheduler;
    }

}

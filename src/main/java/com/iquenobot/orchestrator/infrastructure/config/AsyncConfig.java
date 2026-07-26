package com.iquenobot.orchestrator.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Configuration for async event processing in the Orchestrator.
 * Ensures event listeners don't block the main processing flow.
 */
@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig implements AsyncConfigurer {

    @Bean(name = "orchestratorEventExecutor")
    public Executor orchestratorEventExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Core pool size - always maintained
        executor.setCorePoolSize(5);
        
        // Max pool size - can grow to this if needed
        executor.setMaxPoolSize(20);
        
        // Queue capacity - tasks wait here when all threads busy
        executor.setQueueCapacity(500);
        
        // Thread name prefix for easy identification in logs
        executor.setThreadNamePrefix("orchestrator-event-");
        
        // Keep alive time for idle threads above core pool size
        executor.setKeepAliveSeconds(60);
        
        // Wait for tasks to complete on shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        
        // Rejection policy - log and discard when queue is full
        executor.setRejectedExecutionHandler(new LoggingRejectedExecutionHandler());
        
        executor.initialize();
        
        log.info("Orchestrator event executor initialized: core={} max={} queue={}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());
        
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return orchestratorEventExecutor();
    }

    /**
     * Custom rejection handler that logs rejected tasks
     */
    private static class LoggingRejectedExecutionHandler implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            log.error("Task rejected from orchestrator event executor. " +
                     "Active: {} Queue: {} Pool: {}",
                    executor.getActiveCount(),
                    executor.getQueue().size(),
                    executor.getPoolSize());
            
            // Optionally, you could:
            // 1. Run in caller thread (CallerRunsPolicy)
            // 2. Store in database for later processing
            // 3. Send to dead letter queue
        }
    }
}

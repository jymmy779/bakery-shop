package com.bakery.order.service.impl;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration(proxyBeanMethods = false)
public class OrderMailExecutorConfig {
  @Bean("orderConfirmationExecutor")
  public ThreadPoolTaskExecutor orderConfirmationExecutor() {
    var executor = new ThreadPoolTaskExecutor();
    executor.setThreadNamePrefix("order-mail-");
    executor.setCorePoolSize(1);
    executor.setMaxPoolSize(2);
    executor.setQueueCapacity(100);
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(5);
    return executor;
  }
}

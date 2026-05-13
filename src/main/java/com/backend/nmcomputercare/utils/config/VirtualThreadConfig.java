package com.backend.nmcomputercare.utils.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class VirtualThreadConfig {

    @Bean(destroyMethod = "shutdown")
    public ExecutorService controllerExecutorService() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}

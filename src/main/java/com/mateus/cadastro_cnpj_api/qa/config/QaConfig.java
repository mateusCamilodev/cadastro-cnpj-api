package com.mateus.cadastro_cnpj_api.qa.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

// Infra do QA Center: execução assíncrona com filas LIMITADAS (nunca roda teste dentro da requisição HTTP).
//  - qaExecucoes: uma thread por execução (orquestra os cenários); tamanho = limite de execuções simultâneas
//  - qaCenarios:  roda os cenários em paralelo (até 4 por execução)
@Configuration
@EnableAsync
@EnableConfigurationProperties(QaProperties.class)
public class QaConfig {

    @Bean(name = "qaExecucoes")
    public ThreadPoolTaskExecutor qaExecucoes(QaProperties properties) {
        int limite = properties.limitesOuPadrao().simultaneasOuPadrao();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("qa-execucao-");
        executor.setCorePoolSize(limite);
        executor.setMaxPoolSize(limite);
        executor.setQueueCapacity(limite);
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.initialize();
        return executor;
    }

    @Bean(name = "qaCenarios")
    public ThreadPoolTaskExecutor qaCenarios(QaProperties properties) {
        int limite = properties.limitesOuPadrao().simultaneasOuPadrao() * 4;
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("qa-cenario-");
        executor.setCorePoolSize(limite);
        executor.setMaxPoolSize(limite);
        executor.setQueueCapacity(200);
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.initialize();
        return executor;
    }
}

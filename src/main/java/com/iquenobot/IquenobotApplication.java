package com.iquenobot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.iquenobot")
@EnableConfigurationProperties
@EnableAsync
@EnableScheduling
public class IquenobotApplication {

    public static void main(String[] args) {
        SpringApplication.run(IquenobotApplication.class, args);
    }

}
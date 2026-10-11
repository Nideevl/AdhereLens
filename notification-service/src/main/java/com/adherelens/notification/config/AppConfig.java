package com.adherelens.notification.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

@Configuration
@EnableScheduling
public class AppConfig {

    /** One clock for the whole service. Instants do not depend on the zone, so UTC is fine. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}

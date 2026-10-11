package com.adherelens.clinical.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
@EnableScheduling
public class SchedulingConfig {

    /** One clock for the whole service, in the hospital time zone. Tests can replace it. */
    @Bean
    public Clock clock(@Value("${adherelens.dose.zone}") String zone) {
        return Clock.system(ZoneId.of(zone));
    }
}

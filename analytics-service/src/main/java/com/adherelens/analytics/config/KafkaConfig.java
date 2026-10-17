package com.adherelens.analytics.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {

    /**
     * A failed event (for example clinical-service being down for a moment) is retried every
     * 3 seconds for about a minute before it is skipped. A message that cannot be read at all is
     * skipped straight away, since retrying cannot fix it.
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler() {
        DefaultErrorHandler handler = new DefaultErrorHandler(new FixedBackOff(3_000L, 20L));
        handler.addNotRetryableExceptions(JsonProcessingException.class);
        return handler;
    }
}

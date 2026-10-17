package com.adherelens.clinical.config;

import com.adherelens.common.event.Topics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic doseDueTopic() {
        return TopicBuilder.name(Topics.DOSE_DUE).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic doseOutcomeTopic() {
        return TopicBuilder.name(Topics.DOSE_OUTCOME).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic sideEffectTopic() {
        return TopicBuilder.name(Topics.SIDE_EFFECT).partitions(1).replicas(1).build();
    }
}

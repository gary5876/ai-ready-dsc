package com.aiready.global.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic evalRequestTopic() {
        return TopicBuilder.name("eval-request")
                .partitions(4)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic evalResultTopic() {
        return TopicBuilder.name("eval-result")
                .partitions(4)
                .replicas(1)
                .build();
    }
}

package com.anysale.lead.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicsConfig {
    @Bean NewTopic leadCreated() { return TopicBuilder.name("lead.created").partitions(3).replicas(1).build(); }
    @Bean NewTopic leadUpdated() { return TopicBuilder.name("lead.updated").partitions(3).replicas(1).build(); }
}

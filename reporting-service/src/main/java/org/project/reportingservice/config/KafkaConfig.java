package org.project.reportingservice.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.project.issueevents.events.IssueUpsertedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
public class KafkaConfig {

  @Value("${spring.kafka.bootstrap-servers}")
  private String bootstrapServers;

  @Bean
  public ConsumerFactory<String, IssueUpsertedEvent> consumerFactory() {
    Map<String, Object> props = new HashMap<>();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "reporting-group");
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

    // key deserializer
    StringDeserializer stringDeserializer = new StringDeserializer();

    // value deserializer (JSON -> Java Object)
    JsonDeserializer<IssueUpsertedEvent> jsonDeserializer = new JsonDeserializer<>(IssueUpsertedEvent.class);
    jsonDeserializer.addTrustedPackages("*"); // Trust the issue-events package
    jsonDeserializer.setUseTypeHeaders(false); // Useful if package names slightly differ in docker

    return new DefaultKafkaConsumerFactory<>(
            props,
            new ErrorHandlingDeserializer<>(stringDeserializer),
            new ErrorHandlingDeserializer<>(jsonDeserializer)
    );
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, IssueUpsertedEvent> kafkaListenerContainerFactory() {
    ConcurrentKafkaListenerContainerFactory<String, IssueUpsertedEvent> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory());
    return factory;
  }
}
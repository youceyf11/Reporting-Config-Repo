package org.project.jirafetchservice.kafka;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;

@Configuration
public class KafkaConfig {

  @Value("${spring.kafka.bootstrap-servers}")
  private String bootstrap;

  @Bean
  public SenderOptions<String, String> senderOptions() {
    Map<String, Object> props = new HashMap<>();
    props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    return SenderOptions.create(props);
  }

  @Bean
  public KafkaSender<String, String> kafkaSender(SenderOptions<String, String> senderOptions) {
    return KafkaSender.create(senderOptions);
  }
}

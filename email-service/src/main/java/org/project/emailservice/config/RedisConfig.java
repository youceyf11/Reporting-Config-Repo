package org.project.emailservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.project.emailservice.entity.EmailLog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@Profile("!test")
public class RedisConfig {

  @Bean
  public ReactiveRedisTemplate<String, EmailLog> reactiveRedisTemplate(
      ReactiveRedisConnectionFactory connectionFactory) {

    // Create an ObjectMapper and register the JavaTimeModule
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    // Disable writing dates as timestamps
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // Create a serializer for EmailLog objects
    Jackson2JsonRedisSerializer<EmailLog> serializer =
        new Jackson2JsonRedisSerializer<>(objectMapper, EmailLog.class);

    RedisSerializationContext.RedisSerializationContextBuilder<String, EmailLog> builder =
        RedisSerializationContext.newSerializationContext(new StringRedisSerializer());

    RedisSerializationContext<String, EmailLog> context = builder.value(serializer).build();

    return new ReactiveRedisTemplate<>(connectionFactory, context);
  }
}

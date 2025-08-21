package org.project.reportingservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.project.reportingservice.dto.ReportingResultDto;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

  /** ReactiveRedisTemplate with proper Jackson date/time support for ReportingResultDto. */
  @Bean
  public ReactiveRedisTemplate<String, ReportingResultDto> reportingResultRedisTemplate(
      ReactiveRedisConnectionFactory connectionFactory) {
    // Configure ObjectMapper with JavaTimeModule
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // Serializer for values
    Jackson2JsonRedisSerializer<ReportingResultDto> valueSerializer =
        new Jackson2JsonRedisSerializer<>(ReportingResultDto.class);
    valueSerializer.setObjectMapper(mapper);

    // Build serialization context
    RedisSerializationContext<String, ReportingResultDto> context =
        RedisSerializationContext.<String, ReportingResultDto>newSerializationContext(
                StringRedisSerializer.UTF_8)
            .value(valueSerializer)
            .build();

    return new ReactiveRedisTemplate<>(connectionFactory, context);
  }
}

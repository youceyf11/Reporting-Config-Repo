package org.project.emailservice;

import static org.mockito.Mockito.mock;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.project.emailservice.entity.EmailLog;
import org.project.emailservice.provider.EmailProvider;
import org.project.emailservice.service.EmailMessageConsumer;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.thymeleaf.TemplateEngine;
import reactor.rabbitmq.Receiver;
import reactor.rabbitmq.Sender;

@SpringBootTest
@ActiveProfiles("test")
class EmailServiceApplicationTests {

  @MockBean private EmailMessageConsumer emailMessageConsumer;

  @Test
  void contextLoads() {}

  @TestConfiguration
  static class TestBeans {
    @Bean
    AmqpAdmin amqpAdmin() {
      return mock(AmqpAdmin.class);
    }

    @Bean
    RabbitTemplate rabbitTemplate() {
      return mock(RabbitTemplate.class);
    }

    @Bean
    ReactiveRedisTemplate<String, EmailLog> reactiveRedisTemplate() {
      return mock(ReactiveRedisTemplate.class);
    }

    @Bean
    Sender sender() {
      return mock(Sender.class);
    }

    @Bean
    Receiver receiver() {
      return mock(Receiver.class);
    }

    @Bean
    @Primary
    TemplateEngine templateEngine() {
      return mock(TemplateEngine.class);
    }

    @Bean
    JavaMailSender javaMailSender() {
      return mock(JavaMailSender.class);
    }

    @Bean
    @Primary
    CircuitBreakerRegistry circuitBreakerRegistry() {
      return mock(CircuitBreakerRegistry.class);
    }

    @Bean
    List<EmailProvider> emailProviders() {
      return List.of();
    }
  }
}

package org.project.emailservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.rabbitmq.Sender;
import reactor.rabbitmq.Receiver;
import org.thymeleaf.TemplateEngine;
import org.project.emailservice.entity.EmailLog;
import org.springframework.mail.javamail.JavaMailSender;
import java.util.List;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.project.emailservice.provider.EmailProvider;
import org.project.emailservice.service.EmailMessageConsumer;

import static org.mockito.Mockito.mock;

@SpringBootTest
@ActiveProfiles("test")
class EmailServiceApplicationTests {

    @MockBean
    private EmailMessageConsumer emailMessageConsumer;

    @Test 
    void contextLoads() { }

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
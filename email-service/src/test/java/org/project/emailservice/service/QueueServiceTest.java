package org.project.emailservice.service;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.project.emailservice.dto.EmailRequest;
import org.project.emailservice.enums.EmailPriority;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.test.StepVerifier;

class QueueServiceTest {

  @Mock RabbitTemplate rabbitTemplate;
  @Mock reactor.rabbitmq.Sender sender;
  @Spy ObjectMapper mapper = new ObjectMapper();

  @InjectMocks QueueService queueService;

  @BeforeEach
  void init() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void queueEmail_sends_to_high_routing_key() {
    EmailRequest req = EmailRequest.builder().priority(EmailPriority.HIGH).build();

    ReflectionTestUtils.setField(queueService, "urgentPriorityRoutingKey", "email.high");
    ReflectionTestUtils.setField(queueService, "highPriorityRoutingKey", "email.high");
    ReflectionTestUtils.setField(queueService, "normalPriorityRoutingKey", "email.normal");
    ReflectionTestUtils.setField(queueService, "lowPriorityRoutingKey", "email.low");
    ReflectionTestUtils.setField(queueService, "exchangeName", "email.exchange");

    StepVerifier.create(queueService.queueEmail(req, "id1")).verifyComplete();

    verify(rabbitTemplate)
        .convertAndSend(eq("email.exchange"), eq("email.high"), /* payload JSON */ anyString());
  }
}

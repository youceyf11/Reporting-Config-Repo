package org.project.emailservice.service;

import static org.mockito.Mockito.*;

import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.project.emailservice.entity.EmailLog;
import org.project.emailservice.enums.EmailStatus;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class EmailServiceUpdateStatusTest {

  @Mock ReactiveRedisTemplate<String, EmailLog> redis;
  @Mock ReactiveValueOperations<String, EmailLog> ops;
  @Mock QueueService queueService;
  @Mock TemplateService templateService;
  @Mock com.fasterxml.jackson.databind.ObjectMapper mapper;

  EmailService service;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    when(redis.opsForValue()).thenReturn(ops);
    service = new EmailService(redis, queueService, templateService, mapper);
  }

  @Test
  void updateEmailStatus_sets_new_status() {
    EmailLog log =
        EmailLog.builder()
            .id("id123")
            .status(EmailStatus.QUEUED)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

    when(ops.get("email:id123")).thenReturn(Mono.just(log));
    when(ops.set(anyString(), any(EmailLog.class), any())).thenReturn(Mono.just(true));

    StepVerifier.create(service.updateEmailStatus("id123", EmailStatus.SENT, null))
        .verifyComplete();

    assert log.getStatus() == EmailStatus.SENT;
  }
}

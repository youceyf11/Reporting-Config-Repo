package org.project.emailservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.project.emailservice.dto.EmailRequest;
import org.project.emailservice.dto.EmailResponse;
import org.project.emailservice.entity.EmailLog;
import org.project.emailservice.enums.EmailPriority;
import org.project.emailservice.enums.EmailStatus;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class EmailServiceTest {

  @Mock ReactiveRedisTemplate<String, EmailLog> redisTemplate;
  @Mock QueueService queueService;
  @Mock TemplateService templateService;
  @Mock com.fasterxml.jackson.databind.ObjectMapper mapper;
  @Mock ReactiveValueOperations<String, EmailLog> valueOps;

  EmailService service;

  @BeforeEach
  void setUp() {
    when(redisTemplate.opsForValue()).thenReturn(valueOps);
    service = new EmailService(redisTemplate, queueService, templateService, mapper);
  }

  @Test
  void processEmailRequest_persists_and_queues_request() {
    // given
    EmailRequest req =
        EmailRequest.builder()
            .to("user@test.com")
            .from("me@test.com")
            .subject("hello")
            .priority(EmailPriority.HIGH)
            .metadata(Map.of("k", "v"))
            .build();

    when(valueOps.set(anyString(), any(EmailLog.class))).thenReturn(Mono.just(true));
    when(queueService.queueEmail(any(), anyString())).thenReturn(Mono.empty());

    // when
    Mono<EmailResponse> result = service.processEmailRequest(req);

    // then
    StepVerifier.create(result)
        .assertNext(
            res -> {
              assertThat(res.getStatus()).isEqualTo(EmailStatus.QUEUED);
              assertThat(res.getEmailId()).isNotBlank();
            })
        .verifyComplete();

    verify(queueService).queueEmail(eq(req), anyString());
  }

  @Test
  void getEmailStatus_returns_not_found_when_absent() {
    String id = UUID.randomUUID().toString();
    when(valueOps.get("email:" + id)).thenReturn(Mono.empty());

    StepVerifier.create(service.getEmailStatus(id))
        .assertNext(
            res -> {
              assertThat(res.getEmailId()).isEqualTo(id);
              assertThat(res.getStatus()).isEqualTo(EmailStatus.NOT_FOUND);
            })
        .verifyComplete();
  }
}

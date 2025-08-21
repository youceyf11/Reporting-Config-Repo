package org.project.emailservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.emailservice.dto.EmailRequest;
import org.project.emailservice.dto.EmailResponse;
import org.project.emailservice.entity.EmailLog;
import org.project.emailservice.enums.EmailPriority;
import org.project.emailservice.enums.EmailStatus;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

  private final ReactiveRedisTemplate<String, EmailLog> redisTemplate;
  private final QueueService queueService;
  private final TemplateService templateService;
  private final ObjectMapper objectMapper;

  public Mono<EmailResponse> processEmailRequest(EmailRequest request) {
    String emailId = UUID.randomUUID().toString();
    EmailLog emailLog =
        EmailLog.builder()
            .id(emailId)
            .to(request.getTo())
            .from(request.getFrom())
            .subject(request.getSubject())
            .status(EmailStatus.QUEUED)
            .priority(request.getPriority() != null ? request.getPriority() : EmailPriority.NORMAL)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .metadata(request.getMetadata())
            .build();

    return redisTemplate
        .opsForValue()
        .set("email:" + emailId, emailLog)
        .then(queueService.queueEmail(request, emailId))
        .thenReturn(buildEmailResponse(emailLog));
  }

  public Mono<EmailResponse> sendChartEmail(
      String to, String subject, Map<String, Object> chartData, byte[] chartImage) {

    return templateService
        .renderChartEmailTemplate(chartData)
        .flatMap(
            htmlContent -> {
              Map<String, Object> templateData = new HashMap<>();
              templateData.put("htmlContent", htmlContent);

              EmailRequest emailRequest =
                  EmailRequest.builder()
                      .to(to)
                      .from("noreply@yourcompany.com")
                      .subject(subject)
                      .templateData(templateData)
                      .attachments(
                          List.of(
                              org.project.emailservice.dto.EmailAttachment.builder()
                                  .filename("chart.png")
                                  .content(chartImage)
                                  .contentType("image/png")
                                  .size(chartImage.length)
                                  .build()))
                      .priority(org.project.emailservice.enums.EmailPriority.NORMAL)
                      .build();

              return processEmailRequest(emailRequest);
            });
  }

  public Mono<EmailResponse> getEmailStatus(String emailId) {
    return redisTemplate
        .opsForValue()
        .get("email:" + emailId)
        .map(obj -> objectMapper.convertValue(obj, EmailLog.class))
        .map(this::buildEmailResponse)
        .switchIfEmpty(Mono.fromCallable(() -> buildNotFoundResponse(emailId)));
  }

  public Flux<EmailResponse> getBulkEmailStatus(List<String> emailIds) {
    if (emailIds == null || emailIds.isEmpty()) {
      return Flux.empty();
    }
    List<String> redisKeys =
        emailIds.stream().map(id -> "email:" + id).collect(Collectors.toList());

    return redisTemplate
        .opsForValue()
        .multiGet(redisKeys)
        .flatMapMany(Flux::fromIterable)
        .filter(Objects::nonNull)
        .map(obj -> objectMapper.convertValue(obj, EmailLog.class))
        .map(this::buildEmailResponse);
  }

  public Mono<Void> updateEmailStatus(String emailId, EmailStatus status, String errorMessage) {
    return redisTemplate
        .opsForValue()
        .get("email:" + emailId)
        .flatMap(
            emailLog -> {
              if (emailLog == null) {
                return Mono.error(new RuntimeException("EmailLog not found for id: " + emailId));
              }
              emailLog.setStatus(status);
              emailLog.setUpdatedAt(Instant.now());
              if (errorMessage != null) {
                emailLog.setErrorMessage(errorMessage);
              }
              return redisTemplate
                  .opsForValue()
                  .set("email:" + emailId, emailLog, Duration.ofHours(24));
            })
        .then();
  }

  public EmailResponse buildEmailResponse(EmailLog emailLog) {
    return EmailResponse.builder()
        .emailId(emailLog.getId())
        .status(emailLog.getStatus())
        .errorMessage(emailLog.getErrorMessage())
        .priority(emailLog.getPriority() != null ? emailLog.getPriority().name() : null)
        .createdAt(emailLog.getCreatedAt())
        .updatedAt(emailLog.getUpdatedAt())
        .processedAt(emailLog.getProcessedAt())
        .metadata(emailLog.getMetadata() != null ? Map.copyOf(emailLog.getMetadata()) : null)
        .build();
  }

  public EmailResponse buildNotFoundResponse(String emailId) {
    return new EmailResponse(emailId, EmailStatus.NOT_FOUND, null, null, null, null);
  }
}

package org.project.emailservice.entity;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.project.emailservice.enums.EmailPriority;
import org.project.emailservice.enums.EmailStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RedisHash("EmailLog")
public class EmailLog implements Serializable {
  @Id private String id;
  private String to;
  private String from;
  private String subject;
  private EmailStatus status;
  private EmailPriority priority;
  private Instant createdAt;
  private Instant updatedAt;
  private Instant processedAt;
  private String providerId;
  private String messageId;
  private String errorMessage;
  private int retryCount;
  private Map<String, String> metadata;

  public EmailLog(String id, String to, EmailStatus status) {
    this.id = id;
    this.to = to;
    this.status = status;
    this.createdAt = Instant.now();
    this.updatedAt = Instant.now();
    this.priority = EmailPriority.NORMAL;
  }
}

package org.project.emailservice.dto;

import java.time.Instant;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.project.emailservice.enums.EmailStatus;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailResponse {

  private String emailId;
  private EmailStatus status;
  private String errorMessage;
  private String message;
  private String priority;
  private Instant createdAt;
  private Instant updatedAt;
  private Instant processedAt;
  private Map<String, Object> metadata;

  public EmailResponse(
      String emailId,
      EmailStatus status,
      String errorMessage,
      String priority,
      Instant createdAt,
      Instant updatedAt) {
    this.emailId = emailId;
    this.status = status;
    this.errorMessage = errorMessage;
    this.priority = priority;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }
}

package org.project.emailservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.project.emailservice.enums.EmailPriority;

@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class EmailRequest {

  private String id;

  @NotBlank(message = "Email recipient is required")
  @Email(message = "Invalid email format")
  private String to;

  private String from;

  @NotBlank(message = "Subject is required")
  private String subject;

  private String templateName;
  private Map<String, Object> templateData;
  private List<EmailAttachment> attachments;
  private EmailPriority priority;
  private Instant scheduledAt;
  private Map<String, String> metadata;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getTo() {
    return to;
  }

  public void setTo(String to) {
    this.to = to;
  }

  public String getFrom() {
    return from;
  }

  public void setFrom(String from) {
    this.from = from;
  }

  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  public String getTemplateName() {
    return templateName;
  }

  public void setTemplateName(String templateName) {
    this.templateName = templateName;
  }

  public Map<String, Object> getTemplateData() {
    return templateData != null ? new HashMap<>(templateData) : null;
  }

  public void setTemplateData(Map<String, Object> templateData) {
    this.templateData = templateData != null ? new HashMap<>(templateData) : null;
  }

  public List<EmailAttachment> getAttachments() {
    return attachments != null ? new ArrayList<>(attachments) : null;
  }

  public void setAttachments(List<EmailAttachment> attachments) {
    this.attachments = attachments != null ? new ArrayList<>(attachments) : null;
  }

  public EmailPriority getPriority() {
    return priority;
  }

  public void setPriority(EmailPriority priority) {
    this.priority = priority;
  }

  public Instant getScheduledAt() {
    return scheduledAt;
  }

  public void setScheduledAt(Instant scheduledAt) {
    this.scheduledAt = scheduledAt;
  }

  public Map<String, String> getMetadata() {
    return metadata != null ? new HashMap<>(metadata) : null;
  }

  public void setMetadata(Map<String, String> metadata) {
    this.metadata = metadata != null ? new HashMap<>(metadata) : null;
  }
}

package org.project.emailservice.entity;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailTemplate {
  private String id;
  private String name;
  private String subject;
  private String htmlContent;
  private String textContent;
  private String description;
  private Instant createdAt;
  private Instant updatedAt;
  private boolean active;
}

package org.project.emailservice.dto;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class EmailMessagePayload implements Serializable {
  private String emailId;
  // Marked transient to avoid serialization issues and SpotBugs SE_BAD_FIELD
  private transient EmailRequest request;
  private String routingKey;
}

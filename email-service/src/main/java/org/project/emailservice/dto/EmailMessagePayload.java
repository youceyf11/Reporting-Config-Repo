package org.project.emailservice.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class EmailMessagePayload implements Serializable {
    private String emailId;
    // Marked transient to avoid serialization issues and SpotBugs SE_BAD_FIELD
    private transient EmailRequest request;
    private String routingKey;
}
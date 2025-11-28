package org.project.excelservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailRequestDto implements Serializable {
    private String to;
    private String subject;
    private String body;
    private String attachmentName; // e.g., "chart.png" or "report.xlsx"
    private byte[] attachmentData; // The raw file
}
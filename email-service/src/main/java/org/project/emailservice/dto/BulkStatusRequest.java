package org.project.emailservice.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkStatusRequest {
    
    @NotEmpty(message = "Email IDs list cannot be empty")
    private List<String> emailIds;
}

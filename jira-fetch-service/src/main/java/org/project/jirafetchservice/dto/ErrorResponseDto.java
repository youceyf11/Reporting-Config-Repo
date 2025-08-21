package org.project.jirafetchservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponseDto {
  private String error;
  private String message;
  private String timestamp;
  private Integer status;
}

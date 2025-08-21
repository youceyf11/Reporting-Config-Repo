package org.project.emailservice.enums;

public enum EmailStatus {
  QUEUED,
  PROCESSING,
  SENT,
  FAILED,
  RETRY,
  CANCELLED,
  NOT_FOUND
}

package org.project.jirafetchservice.exception;

public class JiraApiException extends RuntimeException {
  public JiraApiException(String message) {
    super(message);
  }
}

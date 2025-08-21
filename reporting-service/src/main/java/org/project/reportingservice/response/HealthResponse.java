package org.project.reportingservice.response;

/** Classe Integererne pour la réponse de santé du service */
public class HealthResponse {
  private String status;
  private String message;

  public HealthResponse(String status, String message) {
    this.status = status;
    this.message = message;
  }

  // Getters
  public String getStatus() {
    return status;
  }

  public String getMessage() {
    return message;
  }

  // Setters
  public void setStatus(String status) {
    this.status = status;
  }

  public void setMessage(String message) {
    this.message = message;
  }
}

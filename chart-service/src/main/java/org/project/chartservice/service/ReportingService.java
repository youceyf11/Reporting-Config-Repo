package org.project.chartservice.service;

import org.project.chartservice.IService.IReportingService;
import org.project.chartservice.dto.ReportingResultDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class ReportingService implements IReportingService {

  private final WebClient reportingWebClient;

  @Autowired
  public ReportingService(WebClient reportingWebClient) {
    this.reportingWebClient = reportingWebClient;
  }

  @Override
  public Mono<ReportingResultDto> getMonthlyReportingData(String projectKey) {
    return reportingWebClient
        .get()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path("/api/reporting/monthly")
                    .queryParam("projectKey", projectKey)
                    .build())
        .retrieve()
        .bodyToMono(ReportingResultDto.class)
        .onErrorMap(
            ex ->
                new RuntimeException(
                    "Failed to fetch reporting data for project: " + projectKey, ex));
  }
}

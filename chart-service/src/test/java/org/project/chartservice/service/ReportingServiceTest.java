package org.project.chartservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.project.chartservice.dto.ReportingResultDto;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

class ReportingServiceTest {

  private static MockWebServer server;
  private static ReportingService reportingService;
  private static final ObjectMapper MAPPER = new ObjectMapper();

  @BeforeAll
  static void init() throws IOException {
    server = new MockWebServer();
    server.start();
    WebClient client = WebClient.builder().baseUrl(server.url("/").toString()).build();
    reportingService = new ReportingService(client);
  }

  @AfterAll
  static void shutdown() throws IOException {
    server.shutdown();
  }

  @Test
  void getMonthlyReportingData_returnsDto() throws Exception {
    ReportingResultDto dto = new ReportingResultDto("AUG", "2025", "PROJ", List.of());
    server.enqueue(
        new MockResponse()
            .setBody(MAPPER.writeValueAsString(dto))
            .addHeader("Content-Type", "application/json"));

    StepVerifier.create(reportingService.getMonthlyReportingData("PROJ"))
        .expectNextMatches(r -> "PROJ".equals(r.getProjectKey()))
        .verifyComplete();
  }
}

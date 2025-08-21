package org.project.excelservice.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.project.excelservice.service.ExcelReportService;
import org.project.excelservice.service.ExcelSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@WebFluxTest(controllers = ExcelController.class)
class ExcelControllerTest {

  @Autowired WebTestClient client;

  @MockBean ExcelSyncService excelSyncService;

  @MockBean ExcelReportService excelReportService;

  @Test
  @DisplayName("GET /api/excel/{projectKey} returns 200 when sync succeeds")
  void sync_success() {
    Mockito.when(excelSyncService.sync("PROJ"))
        .thenReturn(Mono.just("Sync completed successfully"));

    client
        .get()
        .uri("/api/excel/PROJ")
        .accept(MediaType.TEXT_PLAIN)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(String.class)
        .isEqualTo("Sync completed successfully");
  }

  @Test
  @DisplayName("GET /api/excel/{projectKey} returns 500 when service emits error")
  void sync_error() {
    Mockito.when(excelSyncService.sync("ERR")).thenReturn(Mono.error(new RuntimeException("boom")));

    client
        .get()
        .uri("/api/excel/ERR")
        .exchange()
        .expectStatus()
        .is5xxServerError()
        .expectBody(String.class)
        .value(
            body ->
                org.assertj.core.api.Assertions.assertThat(body)
                    .contains("Failed to sync data for project: ERR"));
  }

  @Test
  @DisplayName("GET /api/excel/performance returns Excel file when report generation succeeds")
  void downloadReport_success() {
    byte[] excelData = "fake excel data".getBytes();
    ByteArrayResource resource = new ByteArrayResource(excelData);

    Mockito.when(excelReportService.generateEmployeeReport("PROJ")).thenReturn(resource);

    client
        .get()
        .uri("/api/excel/performance?projectKey=PROJ")
        .exchange()
        .expectStatus()
        .isOk()
        .expectHeader()
        .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        .expectHeader()
        .valueEquals("Content-Disposition", "attachment; filename=\"performance_PROJ.xlsx\"")
        .expectBody(byte[].class)
        .isEqualTo(excelData);
  }

  @Test
  @DisplayName("GET /api/excel/performance handles missing projectKey parameter")
  void downloadReport_missingProjectKey() {
    client.get().uri("/api/excel/performance").exchange().expectStatus().is4xxClientError();
  }

  @Test
  @DisplayName("GET /api/excel/performance handles service error")
  void downloadReport_serviceError() {
    Mockito.when(excelReportService.generateEmployeeReport("ERR"))
        .thenThrow(new RuntimeException("Report generation failed"));

    client
        .get()
        .uri("/api/excel/performance?projectKey=ERR")
        .exchange()
        .expectStatus()
        .is5xxServerError();
  }
}

package org.project.jirafetchservice.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.jirafetchservice.dto.IssueSimpleDto;
import org.project.jirafetchservice.jiraApi.JiraIssueApiResponse;
import org.project.jirafetchservice.mapper.JiraMapper;
import org.project.jirafetchservice.service.JiraIssueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@WebFluxTest(JiraIssueController.class)
@ActiveProfiles("test")
@DisplayName("JiraIssueController Tests")
class JiraIssueControllerTest {

  @Autowired private WebTestClient webTestClient;

  @MockBean private JiraIssueService jiraIssueService;

  @MockBean private JiraMapper jiraMapper;

  private JiraIssueApiResponse mockJiraResponse;
  private IssueSimpleDto mockSimpleDto;

  @BeforeEach
  void setUp() {
    // Mock JiraIssueApiResponse
    mockJiraResponse = new JiraIssueApiResponse();
    mockJiraResponse.setKey("PROJ-123");
    mockJiraResponse.setSummary("Test Issue");

    // Mock IssueSimpleDto
    mockSimpleDto = new IssueSimpleDto();
    mockSimpleDto.setIssueKey("PROJ-123");
    mockSimpleDto.setSummary("Test Issue");
    mockSimpleDto.setStatus("In Progress");
  }

  // ================== TESTS ENDPOINTS ISSUES ==================

  @Test
  @DisplayName("GET /api/jira/issues/{issueKey} - Success")
  void getIssue_shouldReturnIssue_whenValidKey() {
    // Arrange
    when(jiraIssueService.getIssue("PROJ-123")).thenReturn(Mono.just(mockJiraResponse));

    // Act & Assert
    webTestClient
        .get()
        .uri("/api/jira/issues/PROJ-123")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(JiraIssueApiResponse.class)
        .value(
            response -> {
              assert response.getKey().equals("PROJ-123");
              assert response.getSummary().equals("Test Issue");
            });

    verify(jiraIssueService).getIssue("PROJ-123");
  }

  @Test
  @DisplayName("GET /api/jira/issues/{issueKey} - Not Found")
  void getIssue_shouldReturn404_whenIssueNotFound() {
    // Arrange
    when(jiraIssueService.getIssue("NONEXISTENT-123"))
        .thenReturn(Mono.error(new RuntimeException("Issue not found")));

    // Act & Assert
    webTestClient
        .get()
        .uri("/api/jira/issues/NONEXISTENT-123")
        .exchange()
        .expectStatus()
        .isNotFound();

    verify(jiraIssueService).getIssue("NONEXISTENT-123");
  }

  @Test
  @DisplayName("GET /api/jira/issues/{issueKey} - Invalid Format")
  void getIssue_shouldReturn400_whenInvalidKeyFormat() {
    // Act & Assert
    webTestClient
        .get()
        .uri("/api/jira/issues/invalid-key")
        .exchange()
        .expectStatus()
        .isBadRequest();

    verify(jiraIssueService, never()).getIssue(any());
  }

  @Test
  @DisplayName("GET /api/jira/issues/{issueKey}/project - Success")
  void getProjectKeyFromIssue_shouldReturnProjectKey() {
    // Arrange
    when(jiraIssueService.getProjectKeyFromIssue("PROJ-123")).thenReturn(Mono.just("PROJ"));

    // Act & Assert
    webTestClient
        .get()
        .uri("/api/jira/issues/PROJ-123/project")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(String.class)
        .isEqualTo("PROJ");

    verify(jiraIssueService).getProjectKeyFromIssue("PROJ-123");
  }

  // ================== TESTS ENDPOINTS PROJETS ==================

  @Test
  @DisplayName("GET /api/jira/projects/local - Success")
  void getAllLocalProjectKeys_shouldReturnProjectKeys() {
    // Arrange
    when(jiraIssueService.getAllLocalProjectKeys())
        .thenReturn(Flux.just("PROJ1", "PROJ2", "PROJ3"));

    // Act & Assert
    webTestClient
        .get()
        .uri("/api/jira/projects/local")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(String.class)
        .value(
            response -> {
              // The response is concatenated as "PROJ1PROJ2PROJ3"
              assert response.contains("PROJ1");
              assert response.contains("PROJ2");
              assert response.contains("PROJ3");
            });

    verify(jiraIssueService).getAllLocalProjectKeys();
  }

  @Test
  @DisplayName("GET /api/jira/projects/{projectKey}/issues - Success")
  void getProjectIssues_shouldReturnIssues() {
    // Arrange
    when(jiraIssueService.getProjectIssues("PROJ")).thenReturn(Flux.just(mockJiraResponse));
    when(jiraMapper.toSimpleDtoFromApi(mockJiraResponse)).thenReturn(mockSimpleDto);

    // Act & Assert
    webTestClient
        .get()
        .uri("/api/jira/projects/PROJ/issues")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBodyList(IssueSimpleDto.class)
        .hasSize(1)
        .value(
            issues -> {
              assert issues.get(0).getIssueKey().equals("PROJ-123");
            });

    verify(jiraIssueService).getProjectIssues("PROJ");
    verify(jiraMapper).toSimpleDtoFromApi(mockJiraResponse);
  }

  @Test
  @DisplayName("GET /api/jira/projects/{projectKey}/issues - With Limit")
  void getProjectIssues_shouldRespectLimit() {
    // Arrange
    when(jiraIssueService.getProjectIssues("PROJ"))
        .thenReturn(Flux.just(mockJiraResponse, mockJiraResponse, mockJiraResponse));
    when(jiraMapper.toSimpleDtoFromApi(any())).thenReturn(mockSimpleDto);

    // Act & Assert
    webTestClient
        .get()
        .uri("/api/jira/projects/PROJ/issues?limit=2")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBodyList(IssueSimpleDto.class)
        .hasSize(2);

    verify(jiraIssueService).getProjectIssues("PROJ");
  }

  @Test
  @DisplayName("GET /api/jira/projects/{projectKey}/issues - Invalid Limit")
  void getProjectIssues_shouldReturn400_whenInvalidLimit() {
    // Act & Assert
    webTestClient
        .get()
        .uri("/api/jira/projects/PROJ/issues?limit=150")
        .exchange()
        .expectStatus()
        .isBadRequest();

    verify(jiraIssueService, never()).getProjectIssues(any());
  }

  // ================== TESTS ENDPOINTS RECHERCHE ==================

  @Test
  @DisplayName("GET /api/jira/search - Success")
  void searchIssues_shouldReturnSearchResults() {
    // Arrange
    String jql = "project = PROJ";
    when(jiraIssueService.searchIssues(jql)).thenReturn(Flux.just(mockJiraResponse));

    // Act & Assert
    webTestClient
        .get()
        .uri("/api/jira/search?jql={jql}", jql)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBodyList(JiraIssueApiResponse.class)
        .hasSize(1);

    verify(jiraIssueService).searchIssues(jql);
  }

  @Test
  @DisplayName("GET /api/jira/search - Empty JQL")
  void searchIssues_shouldReturn400_whenEmptyJql() {
    // Act & Assert
    webTestClient.get().uri("/api/jira/search?jql=").exchange().expectStatus().isBadRequest();

    verify(jiraIssueService, never()).searchIssues(any());
  }

  @Test
  @DisplayName("GET /api/jira/assignees/{email}/issues - Success")
  void getIssuesAssignedTo_shouldReturnAssignedIssues() {
    // Arrange
    String email = "user@example.com";
    when(jiraIssueService.getIssuesAssignedTo(email)).thenReturn(Flux.just(mockJiraResponse));

    // Act & Assert
    webTestClient
        .get()
        .uri("/api/jira/assignees/{email}/issues", email)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBodyList(JiraIssueApiResponse.class)
        .hasSize(1);

    verify(jiraIssueService).getIssuesAssignedTo(email);
  }

  // ================== TESTS ENDPOINTS SYNCHRONISATION ==================

  @Test
  @DisplayName("POST /api/jira/issues/{issueKey}/sync - Success")
  void syncIssue_shouldReturnSyncedIssue() {
    // Arrange
    when(jiraIssueService.synchroniserIssueAvecJira("PROJ-123"))
        .thenReturn(Mono.just(mockSimpleDto));

    // Act & Assert
    webTestClient
        .post()
        .uri("/api/jira/issues/PROJ-123/sync")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(IssueSimpleDto.class)
        .value(
            issue -> {
              assert issue.getIssueKey().equals("PROJ-123");
            });

    verify(jiraIssueService).synchroniserIssueAvecJira("PROJ-123");
  }

  @Test
  @DisplayName("POST /api/jira/issues/{issueKey}/sync - Error")
  void syncIssue_shouldReturn500_whenSyncFails() {
    // Arrange
    when(jiraIssueService.synchroniserIssueAvecJira("PROJ-123"))
        .thenReturn(Mono.error(new RuntimeException("Sync failed")));

    // Act & Assert
    webTestClient
        .post()
        .uri("/api/jira/issues/PROJ-123/sync")
        .exchange()
        .expectStatus()
        .is5xxServerError();

    verify(jiraIssueService).synchroniserIssueAvecJira("PROJ-123");
  }

  @Test
  @DisplayName("POST /api/jira/projects/{projectKey}/sync - Success")
  void syncProject_shouldReturnSyncedIssues() {
    // Arrange
    when(jiraIssueService.synchroniserProjetAvecJira("PROJ", 50))
        .thenReturn(Flux.just(mockSimpleDto));

    // Act & Assert
    webTestClient
        .post()
        .uri("/api/jira/projects/PROJ/sync")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBodyList(IssueSimpleDto.class)
        .hasSize(1);

    verify(jiraIssueService).synchroniserProjetAvecJira("PROJ", 50);
  }

  @Test
  @DisplayName("POST /api/jira/projects/{projectKey}/sync - Custom Batch Size")
  void syncProject_shouldUseCustomBatchSize() {
    // Arrange
    when(jiraIssueService.synchroniserProjetAvecJira("PROJ", 25))
        .thenReturn(Flux.just(mockSimpleDto));

    // Act & Assert
    webTestClient
        .post()
        .uri("/api/jira/projects/PROJ/sync?batchSize=25")
        .exchange()
        .expectStatus()
        .isOk();

    verify(jiraIssueService).synchroniserProjetAvecJira("PROJ", 25);
  }

  @Test
  @DisplayName("POST /api/jira/search/sync - Success")
  void syncSearch_shouldReturnSyncedResults() {
    // Arrange
    String jql = "project = PROJ";
    when(jiraIssueService.synchroniserSearchAvecJira(jql)).thenReturn(Flux.just(mockSimpleDto));

    // Act & Assert
    webTestClient
        .post()
        .uri("/api/jira/search/sync?jql={jql}", jql)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBodyList(IssueSimpleDto.class)
        .hasSize(1);

    verify(jiraIssueService).synchroniserSearchAvecJira(jql);
  }

  // ================== TESTS ENDPOINTS BASE LOCALE ==================

  @Test
  @DisplayName("GET /api/jira/local/issues/{issueKey} - Success")
  void getLocalIssue_shouldReturnLocalIssue() {
    // Arrange
    when(jiraIssueService.recupererIssueLocale("PROJ-123")).thenReturn(Mono.just(mockSimpleDto));

    // Act & Assert
    webTestClient
        .get()
        .uri("/api/jira/local/issues/PROJ-123")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(IssueSimpleDto.class)
        .value(
            issue -> {
              assert issue.getIssueKey().equals("PROJ-123");
            });

    verify(jiraIssueService).recupererIssueLocale("PROJ-123");
  }

  @Test
  @DisplayName("GET /api/jira/local/issues/{issueKey} - Not Found")
  void getLocalIssue_shouldReturn404_whenNotFound() {
    // Arrange
    when(jiraIssueService.recupererIssueLocale("NONEXISTENT-123"))
        .thenReturn(Mono.error(new RuntimeException("Not found")));

    // Act & Assert
    webTestClient
        .get()
        .uri("/api/jira/local/issues/NONEXISTENT-123")
        .exchange()
        .expectStatus()
        .isNotFound();

    verify(jiraIssueService).recupererIssueLocale("NONEXISTENT-123");
  }

  // ================== TESTS ENDPOINTS UTILITAIRES ==================

  @Test
  @DisplayName("GET /api/jira/health - Success")
  void health_shouldReturnHealthStatus() {
    // Act & Assert
    webTestClient
        .get()
        .uri("/api/jira/health")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(String.class)
        .isEqualTo("Service Jira opérationnel");
  }

  // ================== TESTS DE VALIDATION ==================

  @Test
  @DisplayName("Validation - Empty Path Variable")
  void validation_shouldReturn400_whenEmptyPathVariable() {
    // Act & Assert - Use a truly invalid path that will trigger validation
    webTestClient
        .get()
        .uri("/api/jira/issues//project") // Double slash creates empty path variable
        .exchange()
        .expectStatus()
        .isBadRequest(); // This triggers validation error for empty issueKey
  }

  @Test
  @DisplayName("Validation - Negative Limit")
  void validation_shouldReturn400_whenNegativeLimit() {
    // Act & Assert
    webTestClient
        .get()
        .uri("/api/jira/projects/PROJ/issues?limit=-1")
        .exchange()
        .expectStatus()
        .isBadRequest();
  }

  @Test
  @DisplayName("Validation - Zero Limit")
  void validation_shouldReturn400_whenZeroLimit() {
    // Act & Assert
    webTestClient
        .get()
        .uri("/api/jira/projects/PROJ/issues?limit=0")
        .exchange()
        .expectStatus()
        .isBadRequest();
  }

  // ================== TESTS DE PERFORMANCE ==================

  @Test
  @DisplayName("Performance - Multiple Concurrent Requests")
  void performance_shouldHandleConcurrentRequests() {
    // Arrange
    when(jiraIssueService.getAllLocalProjectKeys())
        .thenReturn(Flux.just("PROJ1", "PROJ2", "PROJ3"));

    // Act & Assert - Simulate 5 concurrent requests
    for (int i = 0; i < 5; i++) {
      webTestClient.get().uri("/api/jira/projects/local").exchange().expectStatus().isOk();
    }

    verify(jiraIssueService, times(5)).getAllLocalProjectKeys();
  }

  @Test
  @DisplayName("Performance - Large Result Set with Limit")
  void performance_shouldHandleLargeResultSetWithLimit() {
    // Arrange
    Flux<JiraIssueApiResponse> largeFlux =
        Flux.range(1, 1000)
            .map(
                i -> {
                  JiraIssueApiResponse response = new JiraIssueApiResponse();
                  response.setKey("PROJ-" + i);
                  return response;
                });

    when(jiraIssueService.getProjectIssues("PROJ")).thenReturn(largeFlux);
    when(jiraMapper.toSimpleDtoFromApi(any())).thenReturn(mockSimpleDto);

    // Act & Assert
    webTestClient
        .get()
        .uri("/api/jira/projects/PROJ/issues?limit=100")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBodyList(IssueSimpleDto.class)
        .hasSize(100);

    verify(jiraIssueService).getProjectIssues("PROJ");
  }
}

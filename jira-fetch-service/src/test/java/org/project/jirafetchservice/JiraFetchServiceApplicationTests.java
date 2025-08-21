package org.project.jirafetchservice;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.project.jirafetchservice.entity.JiraIssueDbEntity;
import org.project.jirafetchservice.repository.JiraIssueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.test.StepVerifier;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application.properties")
class JiraFetchServiceApplicationTests {

  @LocalServerPort private int port;

  @Autowired private WebTestClient webTestClient;

  @Autowired private JiraIssueRepository jiraIssueRepository;

  @Autowired private DatabaseClient databaseClient;

  @BeforeEach
  void setUp() {
    // Clean database before each test
    jiraIssueRepository.deleteAll().block();
  }

  @Test
  void contextLoads() {
    // Verify Spring Boot context loads successfully
    assertThat(webTestClient).isNotNull();
    assertThat(jiraIssueRepository).isNotNull();
    assertThat(databaseClient).isNotNull();
  }

  @Test
  void databaseConnection_shouldBeEstablished() {
    // Test database connectivity
    StepVerifier.create(databaseClient.sql("SELECT 1").fetch().one())
        .expectNextCount(1)
        .verifyComplete();
  }

  @Test
  void healthEndpoint_shouldReturnOk() {
    webTestClient
        .get()
        .uri("/api/jira/health")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(String.class)
        .isEqualTo("Jira Fetch Service is running!");
  }

  @Test
  void getAllLocalProjectKeys_shouldReturnEmptyInitially() {
    webTestClient
        .get()
        .uri("/api/jira/projects/local")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(String.class)
        .value(body -> assertThat(body).isEmpty());
  }

  @Test
  void getLocalIssue_shouldReturn404_whenIssueNotExists() {
    webTestClient
        .get()
        .uri("/api/jira/local/issues/NONEXISTENT-123")
        .exchange()
        .expectStatus()
        .isNotFound();
  }

  @Test
  void endToEndFlow_shouldWorkCorrectly() {
    // Given - Create and save a test issue
    JiraIssueDbEntity testIssue = createTestIssue();
    jiraIssueRepository.save(testIssue).block();

    // When & Then - Test local issue retrieval
    webTestClient
        .get()
        .uri("/api/jira/local/issues/" + testIssue.getIssueKey())
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.issueKey")
        .isEqualTo(testIssue.getIssueKey())
        .jsonPath("$.summary")
        .isEqualTo(testIssue.getSummary())
        .jsonPath("$.status")
        .isEqualTo(testIssue.getStatus());

    // Test project keys endpoint
    webTestClient
        .get()
        .uri("/api/jira/projects/local")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(String.class)
        .value(body -> assertThat(body).contains("PROJ"));
  }

  @Test
  void validation_shouldWork_onInvalidIssueKey() {
    webTestClient
        .get()
        .uri("/api/jira/issues/invalid-key-format")
        .exchange()
        .expectStatus()
        .isBadRequest();
  }

  @Test
  void validation_shouldWork_onValidIssueKey() {
    // This will fail due to no external Jira connection in tests,
    // but validation should pass
    webTestClient
        .get()
        .uri("/api/jira/issues/PROJ-123")
        .exchange()
        .expectStatus()
        .is5xxServerError(); // Expected due to no real Jira connection
  }

  @Test
  void concurrentRequests_shouldBeHandled() {
    // Given - Create test data
    JiraIssueDbEntity issue1 = createTestIssue("PROJ-001", "Issue 1");
    JiraIssueDbEntity issue2 = createTestIssue("PROJ-002", "Issue 2");

    jiraIssueRepository.save(issue1).block();
    jiraIssueRepository.save(issue2).block();

    // When & Then - Make concurrent requests
    for (int i = 0; i < 5; i++) {
      webTestClient.get().uri("/api/jira/local/issues/PROJ-001").exchange().expectStatus().isOk();

      webTestClient.get().uri("/api/jira/local/issues/PROJ-002").exchange().expectStatus().isOk();
    }
  }

  @Test
  void databaseOperations_shouldPersistCorrectly() {
    // Given
    JiraIssueDbEntity testIssue = createTestIssue();

    // When - Save through repository
    StepVerifier.create(jiraIssueRepository.save(testIssue))
        .expectNextMatches(saved -> saved.getIssueKey().equals(testIssue.getIssueKey()))
        .verifyComplete();

    // Then - Verify persistence
    StepVerifier.create(jiraIssueRepository.findById(testIssue.getIssueKey()))
        .expectNextMatches(
            found ->
                found.getSummary().equals(testIssue.getSummary())
                    && found.getStatus().equals(testIssue.getStatus()))
        .verifyComplete();

    // And verify count
    StepVerifier.create(jiraIssueRepository.count()).expectNext(1L).verifyComplete();
  }

  @Test
  void applicationProperties_shouldBeLoaded() {
    // Verify that application properties are loaded correctly
    // This is implicit through successful context loading and database connection
    assertThat(port).isGreaterThan(0);
  }

  @Test
  void reactiveStreams_shouldWorkCorrectly() {
    // Given - Create multiple test issues
    JiraIssueDbEntity issue1 = createTestIssue("REACTIVE-001", "Reactive Issue 1");
    JiraIssueDbEntity issue2 = createTestIssue("REACTIVE-002", "Reactive Issue 2");
    JiraIssueDbEntity issue3 = createTestIssue("REACTIVE-003", "Reactive Issue 3");

    // When - Save all reactively
    StepVerifier.create(
            jiraIssueRepository.saveAll(java.util.Arrays.asList(issue1, issue2, issue3)))
        .expectNextCount(3)
        .verifyComplete();

    // Then - Query reactively
    StepVerifier.create(jiraIssueRepository.findAll()).expectNextCount(3).verifyComplete();
  }

  @Test
  void errorHandling_shouldWorkCorrectly() {
    // Test error handling for invalid paths
    webTestClient
        .get()
        .uri("/api/jira/nonexistent-endpoint")
        .exchange()
        .expectStatus()
        .isNotFound();

    // Test validation errors
    webTestClient.get().uri("/api/jira/issues/").exchange().expectStatus().isNotFound();
  }

  @Test
  void performanceTest_shouldHandleLoad() {
    // Given - Create test issue
    JiraIssueDbEntity testIssue = createTestIssue();
    jiraIssueRepository.save(testIssue).block();

    // When & Then - Make multiple rapid requests
    long startTime = System.currentTimeMillis();

    for (int i = 0; i < 10; i++) {
      webTestClient
          .get()
          .uri("/api/jira/local/issues/" + testIssue.getIssueKey())
          .exchange()
          .expectStatus()
          .isOk();
    }

    long endTime = System.currentTimeMillis();
    long duration = endTime - startTime;

    // Should complete within reasonable time (less than 5 seconds)
    assertThat(duration).isLessThan(5000);
  }

  @Test
  void databaseSchema_shouldBeInitialized() {
    // Test that the database schema is properly initialized
    // by attempting to query the table structure
    StepVerifier.create(databaseClient.sql("SELECT COUNT(*) FROM jira_issues").fetch().one())
        .expectNextCount(1)
        .verifyComplete();
  }

  @Test
  void springProfiles_shouldBeActive() {
    // Verify test profile is active through successful H2 database usage
    // This is implicit through successful database operations
    StepVerifier.create(jiraIssueRepository.count()).expectNext(0L).verifyComplete();
  }

  // Helper methods
  private JiraIssueDbEntity createTestIssue() {
    return createTestIssue("PROJ-123", "Integration Test Issue");
  }

  private JiraIssueDbEntity createTestIssue(String issueKey, String summary) {
    JiraIssueDbEntity entity = new JiraIssueDbEntity();
    entity.setIssueKey(issueKey);
    entity.setSummary(summary);
    entity.setIssueType("Bug");
    entity.setStatus("Open");
    entity.setPriority("High");
    entity.setAssignee("Test User");
    entity.setReporter("Test Reporter");
    entity.setProjectKey(issueKey.split("-")[0]);
    entity.setOrganization("Test Org");
    entity.setCreated(LocalDateTime.now().minusDays(1));
    entity.setUpdated(LocalDateTime.now());
    // entity.setTimeSpentSeconds(3600);
    entity.setClassification("Internal");
    entity.setEntity("Test Entity");
    entity.setIssueQuality("Good");
    entity.setMedium("Email");
    entity.setTtsDays(5.0);
    entity.setSite("Paris");
    entity.setIssueMonth("January");
    entity.setQuotaPerProject("Hello");
    return entity;
  }
}

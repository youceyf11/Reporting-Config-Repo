package org.project.reportingservice.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelOption;
import java.io.IOException;
import java.time.Duration;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.test.StepVerifier;

@DisplayName("JiraClient Tests")
class JiraClientTest {

  private MockWebServer mockWebServer;
  private JiraClient jiraClient;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() throws IOException {
    mockWebServer = new MockWebServer();
    mockWebServer.start();

    objectMapper = new ObjectMapper();

    WebClient webClient = WebClient.builder().baseUrl(mockWebServer.url("/").toString()).build();

    jiraClient = new JiraClient(webClient);
  }

  @AfterEach
  void tearDown() throws IOException {
    mockWebServer.shutdown();
  }

  @Nested
  @DisplayName("fetchProjectIssues() Tests")
  class FetchProjectIssuesTests {

    @Test
    @DisplayName("Should fetch project issues successfully")
    void fetchProjectIssues_shouldReturnIssues_whenValidResponse() throws Exception {
      // Arrange - Mock response with array of IssueSimpleDto
      String mockResponse =
          """
                [
                    {
                        "issueKey": "PROJ-1",
                        "assignee": "alice@company.com",
                        "timeSpentSeconds": 28800,
                        "resolved": "2024-01-15T10:30:00"
                    },
                    {
                        "issueKey": "PROJ-2",
                        "assignee": "bob@company.com",
                        "timeSpentSeconds": 14400,
                        "resolved": "2024-01-16T14:20:00"
                    }
                ]
                """;

      mockWebServer.enqueue(
          new MockResponse().setBody(mockResponse).setHeader("Content-Type", "application/json"));

      // Act & Assert
      StepVerifier.create(jiraClient.fetchProjectIssues("PROJ", 100))
          .assertNext(
              issue -> {
                assertThat(issue.getIssueKey()).isEqualTo("PROJ-1");
                assertThat(issue.getAssignee()).isEqualTo("alice@company.com");
                assertThat(issue.getTimeSpentHours()).isEqualTo(8.0);
                assertThat(issue.getResolved()).isNotNull();
              })
          .assertNext(
              issue -> {
                assertThat(issue.getIssueKey()).isEqualTo("PROJ-2");
                assertThat(issue.getAssignee()).isEqualTo("bob@company.com");
                assertThat(issue.getTimeSpentHours()).isEqualTo(4.0);
              })
          .verifyComplete();

      // Verify request to jira-fetch-service endpoint
      RecordedRequest request = mockWebServer.takeRequest();
      assertThat(request.getPath()).isEqualTo("/api/jira/projects/PROJ/issues?limit=100");
    }

    @Test
    @DisplayName("Should handle empty response")
    void fetchProjectIssues_shouldHandleEmptyResponse() {
      // Arrange
      mockWebServer.enqueue(
          new MockResponse().setBody("[]").setHeader("Content-Type", "application/json"));

      // Act & Assert
      StepVerifier.create(jiraClient.fetchProjectIssues("EMPTY-PROJ", 50)).verifyComplete();
    }

    @Test
    @DisplayName("Should handle null assignee")
    void fetchProjectIssues_shouldHandleNullAssignee() {
      // Arrange
      String responseWithNullAssignee =
          """
                [
                    {
                        "issueKey": "PROJ-1",
                        "assignee": null,
                        "timeSpentSeconds": 7200,
                        "resolved": "2024-01-15T10:30:00"
                    }
                ]
                """;

      mockWebServer.enqueue(
          new MockResponse()
              .setBody(responseWithNullAssignee)
              .setHeader("Content-Type", "application/json"));

      // Act & Assert
      StepVerifier.create(jiraClient.fetchProjectIssues("PROJ", 100))
          .assertNext(
              issue -> {
                assertThat(issue.getIssueKey()).isEqualTo("PROJ-1");
                assertThat(issue.getAssignee()).isNull();
                assertThat(issue.getTimeSpentHours()).isEqualTo(2.0);
              })
          .verifyComplete();
    }

    @Test
    @DisplayName("Should handle HTTP 404 error")
    void fetchProjectIssues_shouldHandleNotFound() {
      // Arrange
      mockWebServer.enqueue(new MockResponse().setResponseCode(404).setBody("Project not found"));

      // Act & Assert - JiraClient has onErrorResume that returns empty Flux
      StepVerifier.create(jiraClient.fetchProjectIssues("NONEXISTENT", 100)).verifyComplete();
    }

    @Test
    @DisplayName("Should handle HTTP 401 unauthorized")
    void fetchProjectIssues_shouldHandleUnauthorized() {
      // Arrange
      mockWebServer.enqueue(new MockResponse().setResponseCode(401).setBody("Unauthorized"));

      // Act & Assert - JiraClient has onErrorResume that returns empty Flux
      StepVerifier.create(jiraClient.fetchProjectIssues("PROJ", 100)).verifyComplete();
    }

    @Test
    @DisplayName("Should handle server timeout")
    void fetchProjectIssues_shouldHandleTimeout() {
      // Use a WebClient pointing to an unreachable address to trigger quick connect/response
      // timeout
      WebClient timeoutClient =
          WebClient.builder()
              .baseUrl("http://127.0.0.1:1") // invalid/unbound port
              .clientConnector(
                  new ReactorClientHttpConnector(
                      HttpClient.create()
                          .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)
                          .responseTimeout(Duration.ofSeconds(2))))
              .build();
      JiraClient timeoutJiraClient = new JiraClient(timeoutClient);

      // Act & Assert - JiraClient has onErrorResume that returns empty Flux on timeout/connect
      // failure
      StepVerifier.create(timeoutJiraClient.fetchProjectIssues("PROJ", 100)).verifyComplete();
    }

    @Test
    @DisplayName("Should handle malformed JSON response")
    void fetchProjectIssues_shouldHandleMalformedJson() {
      // Arrange
      mockWebServer.enqueue(
          new MockResponse()
              .setBody("{ invalid json }")
              .setHeader("Content-Type", "application/json"));

      // Act & Assert - JiraClient has onErrorResume that returns empty Flux
      StepVerifier.create(jiraClient.fetchProjectIssues("PROJ", 100)).verifyComplete();
    }

    @Test
    @DisplayName("Should handle zero time spent")
    void fetchProjectIssues_shouldHandleZeroTimeSpent() {
      // Arrange
      String responseWithZeroTime =
          """
                [
                    {
                        "issueKey": "PROJ-1",
                        "assignee": "alice@company.com",
                        "timeSpentSeconds": 0,
                        "resolved": "2024-01-15T10:30:00"
                    }
                ]
                """;

      mockWebServer.enqueue(
          new MockResponse()
              .setBody(responseWithZeroTime)
              .setHeader("Content-Type", "application/json"));

      // Act & Assert
      StepVerifier.create(jiraClient.fetchProjectIssues("PROJ", 100))
          .assertNext(
              issue -> {
                assertThat(issue.getTimeSpentHours()).isEqualTo(0.0);
              })
          .verifyComplete();
    }
  }

  @Nested
  @DisplayName("fetchIssuesByJql() Tests")
  class FetchIssuesByJqlTests {

    @Test
    @DisplayName("Should fetch issues by JQL successfully")
    void fetchIssuesByJql_shouldReturnIssues_whenValidJql() {
      // Arrange
      String mockResponse =
          """
                [
                    {
                        "issueKey": "TASK-1",
                        "assignee": "developer@company.com",
                        "timeSpentSeconds": 21600,
                        "resolved": "2024-01-20T16:45:00"
                    }
                ]
                """;

      mockWebServer.enqueue(
          new MockResponse().setBody(mockResponse).setHeader("Content-Type", "application/json"));

      String jql = "project = TASK AND status = Done";

      // Act & Assert
      StepVerifier.create(jiraClient.fetchIssuesByJql(jql, 50))
          .assertNext(
              issue -> {
                assertThat(issue.getIssueKey()).isEqualTo("TASK-1");
                assertThat(issue.getAssignee()).isEqualTo("developer@company.com");
                assertThat(issue.getTimeSpentHours()).isEqualTo(6.0);
              })
          .verifyComplete();
    }

    @Test
    @DisplayName("Should handle invalid JQL syntax")
    void fetchIssuesByJql_shouldHandleInvalidJql() {
      // Arrange
      mockWebServer.enqueue(new MockResponse().setResponseCode(400).setBody("Invalid JQL syntax"));

      // Act & Assert - JiraClient has onErrorResume that returns empty Flux
      StepVerifier.create(jiraClient.fetchIssuesByJql("INVALID JQL SYNTAX", 100)).verifyComplete();
    }

    @Test
    @DisplayName("Should encode JQL parameters correctly")
    void fetchIssuesByJql_shouldEncodeJqlCorrectly() throws InterruptedException {
      // Arrange
      mockWebServer.enqueue(
          new MockResponse().setBody("[]").setHeader("Content-Type", "application/json"));

      String jqlWithSpecialChars = "project = \"TEST PROJECT\" AND assignee = \"user@domain.com\"";

      // Act
      StepVerifier.create(jiraClient.fetchIssuesByJql(jqlWithSpecialChars, 100)).verifyComplete();

      // Assert - Verify URL encoding
      RecordedRequest request = mockWebServer.takeRequest();
      assertThat(request.getPath()).contains("/api/jira/search");
      assertThat(request.getPath()).contains("jql=");
      assertThat(request.getPath()).contains("limit=100");
    }
  }

  @Nested
  @DisplayName("Performance and Reliability Tests")
  class PerformanceAndReliabilityTests {

    @Test
    @DisplayName("Should handle large response efficiently")
    void shouldHandleLargeResponse() {
      // Arrange
      StringBuilder largeResponse = new StringBuilder("[");
      for (int i = 0; i < 1000; i++) {
        if (i > 0) largeResponse.append(",");
        largeResponse.append(
            String.format(
                """
                    {
                        "issueKey": "LARGE-%d",
                        "assignee": "user%d@company.com",
                        "timeSpentSeconds": %d,
                        "resolved": "2024-01-15T10:30:00"
                    }
                    """,
                i, i % 100, (i % 10 + 1) * 3600));
      }
      largeResponse.append("]");

      mockWebServer.enqueue(
          new MockResponse()
              .setBody(largeResponse.toString())
              .setHeader("Content-Type", "application/json"));

      // Act & Assert
      StepVerifier.create(jiraClient.fetchProjectIssues("LARGE-PROJ", 1000))
          .expectNextCount(1000)
          .verifyComplete();
    }

    @Test
    @DisplayName("Should handle concurrent requests")
    void shouldHandleConcurrentRequests() {
      // Arrange
      for (int i = 0; i < 3; i++) {
        mockWebServer.enqueue(
            new MockResponse().setBody("[]").setHeader("Content-Type", "application/json"));
      }

      // Act & Assert
      StepVerifier.create(
              jiraClient
                  .fetchProjectIssues("PROJ-1", 100)
                  .mergeWith(jiraClient.fetchProjectIssues("PROJ-2", 100))
                  .mergeWith(jiraClient.fetchProjectIssues("PROJ-3", 100)))
          .verifyComplete();
    }

    @Test
    @DisplayName("Should handle network interruption gracefully")
    void shouldHandleNetworkInterruption() {
      // Arrange
      mockWebServer.enqueue(
          new MockResponse()
              .setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY));

      // Act & Assert - JiraClient has onErrorResume that returns empty Flux
      StepVerifier.create(jiraClient.fetchProjectIssues("PROJ", 100)).verifyComplete();
    }

    @Test
    @DisplayName("Should handle rate limiting (429)")
    void shouldHandleRateLimiting() {
      // Arrange
      mockWebServer.enqueue(
          new MockResponse()
              .setResponseCode(429)
              .setHeader("Retry-After", "60")
              .setBody("Rate limit exceeded"));

      // Act & Assert - JiraClient has onErrorResume that returns empty Flux
      StepVerifier.create(jiraClient.fetchProjectIssues("PROJ", 100)).verifyComplete();
    }
  }

  @Nested
  @DisplayName("Data Mapping Tests")
  class DataMappingTests {

    @Test
    @DisplayName("Should map all issue fields correctly")
    void shouldMapAllFieldsCorrectly() {
      // Arrange
      String completeResponse =
          """
                [
                    {
                        "issueKey": "COMPLETE-1",
                        "assignee": "complete@company.com",
                        "timeSpentSeconds": 36000,
                        "resolved": "2024-01-15T10:30:00"
                    }
                ]
                """;

      mockWebServer.enqueue(
          new MockResponse()
              .setBody(completeResponse)
              .setHeader("Content-Type", "application/json"));

      // Act & Assert
      StepVerifier.create(jiraClient.fetchProjectIssues("PROJ", 100))
          .assertNext(
              issue -> {
                assertThat(issue.getIssueKey()).isEqualTo("COMPLETE-1");
                assertThat(issue.getAssignee()).isEqualTo("complete@company.com");
                assertThat(issue.getTimeSpentHours()).isEqualTo(10.0);
                assertThat(issue.getResolved()).isNotNull();
              })
          .verifyComplete();
    }

    @Test
    @DisplayName("Should handle missing optional fields")
    void shouldHandleMissingOptionalFields() {
      // Arrange
      String minimalResponse =
          """
                [
                    {
                        "issueKey": "MINIMAL-1",
                        "assignee": "minimal@company.com",
                        "timeSpentSeconds": 0,
                        "resolved": null
                    }
                ]
                """;

      mockWebServer.enqueue(
          new MockResponse()
              .setBody(minimalResponse)
              .setHeader("Content-Type", "application/json"));

      // Act & Assert
      StepVerifier.create(jiraClient.fetchProjectIssues("PROJ", 100))
          .assertNext(
              issue -> {
                assertThat(issue.getIssueKey()).isEqualTo("MINIMAL-1");
                assertThat(issue.getAssignee()).isEqualTo("minimal@company.com");
                assertThat(issue.getTimeSpentHours()).isEqualTo(0.0);
                assertThat(issue.getResolved()).isNull();
              })
          .verifyComplete();
    }
  }
}

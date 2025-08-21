package org.project.jirafetchservice.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.jirafetchservice.jiraApi.JiraIssueApiResponse;
import org.project.jirafetchservice.jiraApi.JiraSearchResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class JiraWebClientTest {

  private MockWebServer mockWebServer;
  private JiraWebClient jiraWebClient;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() throws IOException {
    mockWebServer = new MockWebServer();
    mockWebServer.start();

    objectMapper = new ObjectMapper();

    WebClient webClient = WebClient.builder().baseUrl(mockWebServer.url("/").toString()).build();

    jiraWebClient = new JiraWebClient(webClient);
  }

  @AfterEach
  void tearDown() throws IOException {
    mockWebServer.shutdown();
  }

  @Test
  void getIssue_shouldReturnIssue_whenValidIssueKey() throws Exception {
    // Given
    String issueKey = "PROJ-123";
    JiraIssueApiResponse expectedResponse = createMockJiraIssueResponse(issueKey);

    mockWebServer.enqueue(
        new MockResponse()
            .setBody(objectMapper.writeValueAsString(expectedResponse))
            .addHeader("Content-Type", "application/json"));

    // When & Then
    StepVerifier.create(jiraWebClient.getIssue(issueKey))
        .expectNextMatches(
            response -> response.getKey().equals(issueKey) && response.getFields() != null)
        .verifyComplete();

    // Verify request
    RecordedRequest recordedRequest = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
    assertThat(recordedRequest.getPath()).isEqualTo("/rest/api/2/issue/" + issueKey);
    assertThat(recordedRequest.getMethod()).isEqualTo("GET");
  }

  @Test
  void getIssue_shouldHandleError_whenIssueNotFound() {
    // Given
    String issueKey = "NONEXISTENT-123";
    mockWebServer.enqueue(
        new MockResponse()
            .setResponseCode(404)
            .setBody("{\"errorMessages\":[\"Issue does not exist\"]}"));

    // When & Then
    StepVerifier.create(jiraWebClient.getIssue(issueKey))
        .expectError(WebClientResponseException.NotFound.class)
        .verify();
  }

  @Test
  void getIssue_shouldHandleTimeout() {
    // Given
    String issueKey = "TIMEOUT-123";
    // Don't enqueue any response - this will cause the request to hang

    // When & Then
    StepVerifier.create(jiraWebClient.getIssue(issueKey).timeout(Duration.ofSeconds(1)))
        .expectError(java.util.concurrent.TimeoutException.class)
        .verify();
  }

  @Test
  void searchIssues_shouldReturnSearchResults_whenValidJql() throws Exception {
    // Given
    String jql = "project = PROJ";
    JiraSearchResponse expectedResponse = createMockSearchResponse();

    mockWebServer.enqueue(
        new MockResponse()
            .setBody(objectMapper.writeValueAsString(expectedResponse))
            .addHeader("Content-Type", "application/json"));

    // When & Then
    StepVerifier.create(jiraWebClient.searchIssues(jql))
        .expectNextMatches(
            response -> response.getIssues() != null && response.getIssues().size() == 2)
        .verifyComplete();

    // Verify request
    RecordedRequest recordedRequest = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
    assertThat(recordedRequest.getPath()).contains("/rest/api/2/search");
    assertThat(recordedRequest.getPath()).contains("jql=project%20%3D%20PROJ");
  }

  @Test
  void searchIssues_shouldHandleError_whenInvalidJql() {
    // Given
    String invalidJql = "invalid jql syntax";
    mockWebServer.enqueue(
        new MockResponse().setResponseCode(400).setBody("{\"errorMessages\":[\"Invalid JQL\"]}"));

    // When & Then
    StepVerifier.create(jiraWebClient.searchIssues(invalidJql))
        .expectError(WebClientResponseException.BadRequest.class)
        .verify();
  }

  @Test
  void searchProjectIssues_shouldReturnProjectIssues_whenValidProjectKey() throws Exception {
    // Given
    String projectKey = "PROJ";
    JiraSearchResponse searchResponse = createMockSearchResponse();

    mockWebServer.enqueue(
        new MockResponse()
            .setBody(objectMapper.writeValueAsString(searchResponse))
            .addHeader("Content-Type", "application/json"));

    // When & Then
    StepVerifier.create(jiraWebClient.searchProjectIssues(projectKey))
        .expectNextCount(2)
        .verifyComplete();

    // Verify JQL construction
    RecordedRequest recordedRequest = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
    assertThat(recordedRequest.getPath()).contains("jql=project%20%3D%20" + projectKey);
  }

  @Test
  void getAllProjects_shouldReturnProjects_whenSuccessful() throws Exception {
    // Given
    JiraIssueApiResponse[] projects = {
      createMockJiraIssueResponse("PROJ1-1"), createMockJiraIssueResponse("PROJ2-1")
    };

    mockWebServer.enqueue(
        new MockResponse()
            .setBody(objectMapper.writeValueAsString(projects))
            .addHeader("Content-Type", "application/json"));

    // When & Then
    StepVerifier.create(jiraWebClient.getAllProjects()).expectNextCount(2).verifyComplete();

    // Verify request
    RecordedRequest recordedRequest = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
    assertThat(recordedRequest.getPath()).isEqualTo("/rest/api/2/project");
  }

  @Test
  void getAllProjects_shouldReturnEmpty_whenError() {
    // Given
    mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("Internal Server Error"));

    // When & Then
    StepVerifier.create(jiraWebClient.getAllProjects())
        .verifyComplete(); // Should return empty due to onErrorResume
  }

  @Test
  void getAllProjects_shouldHandleEmptyResponse() throws Exception {
    // Given
    mockWebServer.enqueue(
        new MockResponse().setBody("[]").addHeader("Content-Type", "application/json"));

    // When & Then
    StepVerifier.create(jiraWebClient.getAllProjects()).verifyComplete();
  }

  @Test
  void searchIssues_shouldHandleSpecialCharactersInJql() throws Exception {
    // Given
    String jqlWithSpecialChars = "summary ~ \"test & special chars\"";
    JiraSearchResponse expectedResponse = createMockSearchResponse();

    mockWebServer.enqueue(
        new MockResponse()
            .setBody(objectMapper.writeValueAsString(expectedResponse))
            .addHeader("Content-Type", "application/json"));

    // When & Then
    StepVerifier.create(jiraWebClient.searchIssues(jqlWithSpecialChars))
        .expectNextMatches(response -> response.getIssues() != null)
        .verifyComplete();
  }

  @Test
  void webClient_shouldHandleMultipleConcurrentRequests() throws Exception {
    // Given
    for (int i = 0; i < 3; i++) {
      mockWebServer.enqueue(
          new MockResponse()
              .setBody(objectMapper.writeValueAsString(createMockJiraIssueResponse("PROJ-" + i)))
              .addHeader("Content-Type", "application/json"));
    }

    // When
    Flux<JiraIssueApiResponse> concurrentRequests =
        Flux.merge(
            jiraWebClient.getIssue("PROJ-1"),
            jiraWebClient.getIssue("PROJ-2"),
            jiraWebClient.getIssue("PROJ-3"));

    // Then
    StepVerifier.create(concurrentRequests).expectNextCount(3).verifyComplete();
  }

  // Helper methods
  private JiraIssueApiResponse createMockJiraIssueResponse(String issueKey) {
    JiraIssueApiResponse response = new JiraIssueApiResponse();
    response.setKey(issueKey);
    response.setSelf("https://jira.example.com/rest/api/2/issue/" + issueKey);

    JiraIssueApiResponse.Fields fields = new JiraIssueApiResponse.Fields();
    fields.setSummary("Test issue summary");

    JiraIssueApiResponse.IssueType issueType = new JiraIssueApiResponse.IssueType();
    issueType.setName("Bug");
    fields.setIssuetype(issueType);

    JiraIssueApiResponse.Status status = new JiraIssueApiResponse.Status();
    status.setName("Open");
    fields.setStatus(status);

    response.setFields(fields);
    return response;
  }

  private JiraSearchResponse createMockSearchResponse() {
    JiraSearchResponse response = new JiraSearchResponse();
    response.setTotal(2);

    response.setIssues(
        Arrays.asList(
            createMockJiraIssueResponse("PROJ-1"), createMockJiraIssueResponse("PROJ-2")));

    return response;
  }
}

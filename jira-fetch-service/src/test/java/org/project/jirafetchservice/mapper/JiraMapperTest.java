package org.project.jirafetchservice.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.project.jirafetchservice.dto.IssueSimpleDto;
import org.project.jirafetchservice.dto.JiraIssueDto;
import org.project.jirafetchservice.jiraApi.JiraIssueApiResponse;

class JiraMapperTest {

  private JiraMapper jiraMapper;

  @BeforeEach
  void setUp() {
    jiraMapper = Mappers.getMapper(JiraMapper.class);
  }

  @Test
  void toSimpleDtoFromApi_shouldMapAllFields_whenValidApiResponse() {
    // Given
    JiraIssueApiResponse apiResponse = createCompleteApiResponse();

    // When
    IssueSimpleDto result = jiraMapper.toSimpleDtoFromApi(apiResponse);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getIssueKey()).isEqualTo("PROJ-123");
    assertThat(result.getSummary()).isEqualTo("Test Summary");
    assertThat(result.getIssueType()).isEqualTo("Bug");
    assertThat(result.getStatus()).isEqualTo("Open");
    assertThat(result.getPriority()).isEqualTo("High");
    assertThat(result.getResolution()).isEqualTo("Fixed");
    assertThat(result.getAssignee()).isEqualTo("John Doe");
    assertThat(result.getReporter()).isEqualTo("Jane Smith");
    assertThat(result.getOrganization()).isEqualTo("Test Org");
    assertThat(result.getTimeSpentSeconds()).isEqualTo(3600L);
    assertThat(result.getClassification()).isEqualTo("Internal");
    assertThat(result.getEntity()).isEqualTo("Test Entity");
    assertThat(result.getIssueQuality()).isEqualTo("Good");
    assertThat(result.getMedium()).isEqualTo("Email");
    assertThat(result.getTtsDays()).isEqualTo(5);
    assertThat(result.getSite()).isEqualTo("Paris");
    assertThat(result.getMonth()).isEqualTo("January");
    assertThat(result.getQuotaPerProject()).isEqualTo("hello");
  }

  @Test
  void toSimpleDtoFromApi_shouldHandleNullFields_whenApiResponseHasNulls() {
    // Given
    JiraIssueApiResponse apiResponse = new JiraIssueApiResponse();
    apiResponse.setKey("PROJ-123");
    apiResponse.setFields(new JiraIssueApiResponse.Fields());

    // When
    IssueSimpleDto result = jiraMapper.toSimpleDtoFromApi(apiResponse);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getIssueKey()).isEqualTo("PROJ-123");
    assertThat(result.getSummary()).isNull();
    assertThat(result.getIssueType()).isNull();
    assertThat(result.getStatus()).isNull();
    assertThat(result.getPriority()).isNull();
    assertThat(result.getAssignee()).isNull();
    assertThat(result.getReporter()).isNull();
  }

  @Test
  void toJiraIssueDto_shouldMapAllFields_whenValidApiResponse() {
    // Given
    JiraIssueApiResponse apiResponse = createCompleteApiResponse();

    // When
    JiraIssueDto result = jiraMapper.toJiraIssueDto(apiResponse);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getIssueKey()).isEqualTo("PROJ-123");
    assertThat(result.getProjectKey()).isEqualTo("PROJ");
    assertThat(result.getSummary()).isEqualTo("Test Summary");
    assertThat(result.getIssueType()).isEqualTo("Bug");
    assertThat(result.getStatus()).isEqualTo("Open");
    assertThat(result.getPriority()).isEqualTo("High");
    assertThat(result.getAssigneeEmail()).isEqualTo("john@example.com");
    assertThat(result.getReporterEmail()).isEqualTo("jane@example.com");
    assertThat(result.getSelf()).isEqualTo("https://jira.example.com/rest/api/2/issue/PROJ-123");
  }

  @Test
  void parseJiraDate_shouldParseValidDate_whenCorrectFormat() {
    // Given
    String validDateString = "2023-12-25T10:30:45.123+0100";

    // When
    LocalDateTime result = jiraMapper.parseJiraDate(validDateString);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getYear()).isEqualTo(2023);
    assertThat(result.getMonthValue()).isEqualTo(12);
    assertThat(result.getDayOfMonth()).isEqualTo(25);
    assertThat(result.getHour()).isEqualTo(10);
    assertThat(result.getMinute()).isEqualTo(30);
    assertThat(result.getSecond()).isEqualTo(45);
  }

  @Test
  void parseJiraDate_shouldReturnNull_whenNullInput() {
    // When
    LocalDateTime result = jiraMapper.parseJiraDate(null);

    // Then
    assertThat(result).isNull();
  }

  @Test
  void parseJiraDate_shouldReturnNull_whenEmptyInput() {
    // When
    LocalDateTime result = jiraMapper.parseJiraDate("");

    // Then
    assertThat(result).isNull();
  }

  @Test
  void parseJiraDate_shouldReturnNull_whenInvalidFormat() {
    // Given
    String invalidDateString = "invalid-date-format";

    // When
    LocalDateTime result = jiraMapper.parseJiraDate(invalidDateString);

    // Then
    assertThat(result).isNull();
  }

  @Test
  void parseJiraDate_shouldHandleDifferentTimezones() {
    // Given
    String dateWithDifferentTimezone = "2023-12-25T10:30:45.123-0500";

    // When
    LocalDateTime result = jiraMapper.parseJiraDate(dateWithDifferentTimezone);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getYear()).isEqualTo(2023);
    assertThat(result.getMonthValue()).isEqualTo(12);
    assertThat(result.getDayOfMonth()).isEqualTo(25);
  }

  @Test
  void extractProjectKey_shouldExtractCorrectKey_whenValidIssueKey() {
    // Given
    String issueKey = "PROJ-123";

    // When
    String result = jiraMapper.extractProjectKey(issueKey);

    // Then
    assertThat(result).isEqualTo("PROJ");
  }

  @Test
  void extractProjectKey_shouldReturnNull_whenNullInput() {
    // When
    String result = jiraMapper.extractProjectKey(null);

    // Then
    assertThat(result).isNull();
  }

  @Test
  void extractProjectKey_shouldReturnNull_whenNoHyphen() {
    // Given
    String issueKeyWithoutHyphen = "PROJ123";

    // When
    String result = jiraMapper.extractProjectKey(issueKeyWithoutHyphen);

    // Then
    assertThat(result).isNull();
  }

  @Test
  void extractProjectKey_shouldHandleMultipleHyphens() {
    // Given
    String issueKeyWithMultipleHyphens = "COMPLEX-PROJECT-123";

    // When
    String result = jiraMapper.extractProjectKey(issueKeyWithMultipleHyphens);

    // Then
    assertThat(result).isEqualTo("COMPLEX");
  }

  @Test
  void formatDateForDb_shouldFormatCorrectly_whenValidDateTime() {
    // Given
    LocalDateTime dateTime = LocalDateTime.of(2023, 12, 25, 10, 30, 45);

    // When
    String result = jiraMapper.formatDateForDb(dateTime);

    // Then
    assertThat(result).isEqualTo("2023-12-25T10:30:45");
  }

  @Test
  void formatDateForDb_shouldReturnNull_whenNullInput() {
    // When
    String result = jiraMapper.formatDateForDb(null);

    // Then
    assertThat(result).isNull();
  }

  @Test
  void toSimpleDtoFromApi_shouldHandleDateParsing_whenValidDates() {
    // Given
    JiraIssueApiResponse apiResponse = new JiraIssueApiResponse();
    apiResponse.setKey("PROJ-123");

    JiraIssueApiResponse.Fields fields = new JiraIssueApiResponse.Fields();
    fields.setCreated("2023-12-25T10:30:45.123+0100");
    fields.setUpdated("2023-12-26T11:30:45.123+0100");
    fields.setResolved("2023-12-27T12:30:45.123+0100");
    apiResponse.setFields(fields);

    // When
    IssueSimpleDto result = jiraMapper.toSimpleDtoFromApi(apiResponse);

    // Then
    assertThat(result.getCreated()).isNotNull();
    assertThat(result.getUpdated()).isNotNull();
    assertThat(result.getResolved()).isNotNull();
    assertThat(result.getCreated().getDayOfMonth()).isEqualTo(25);
    assertThat(result.getUpdated().getDayOfMonth()).isEqualTo(26);
    assertThat(result.getResolved().getDayOfMonth()).isEqualTo(27);
  }

  @Test
  void toSimpleDtoFromApi_shouldHandleInvalidDates_gracefully() {
    // Given
    JiraIssueApiResponse apiResponse = new JiraIssueApiResponse();
    apiResponse.setKey("PROJ-123");

    JiraIssueApiResponse.Fields fields = new JiraIssueApiResponse.Fields();
    fields.setCreated("invalid-date");
    fields.setUpdated("another-invalid-date");
    apiResponse.setFields(fields);

    // When
    IssueSimpleDto result = jiraMapper.toSimpleDtoFromApi(apiResponse);

    // Then
    assertThat(result.getCreated()).isNull();
    assertThat(result.getUpdated()).isNull();
  }

  // Helper method to create complete API response
  private JiraIssueApiResponse createCompleteApiResponse() {
    JiraIssueApiResponse apiResponse = new JiraIssueApiResponse();
    apiResponse.setKey("PROJ-123");
    apiResponse.setSelf("https://jira.example.com/rest/api/2/issue/PROJ-123");

    JiraIssueApiResponse.Fields fields = new JiraIssueApiResponse.Fields();
    fields.setSummary("Test Summary");
    fields.setOrganization("Test Org");
    fields.setCreated("2023-12-25T10:30:45.123+0100");
    fields.setUpdated("2023-12-26T11:30:45.123+0100");
    fields.setResolved("2023-12-27T12:30:45.123+0100");
    fields.setTimeSpentSeconds(3600L);
    fields.setClassification("Internal");
    fields.setEntity("Test Entity");
    fields.setIssueQuality("Good");
    fields.setMedium("Email");
    fields.setTtsDays(5.0);
    fields.setSite("Paris");
    fields.setMonth("January");
    fields.setQuotaPerProject("hello");

    // Issue Type
    JiraIssueApiResponse.IssueType issueType = new JiraIssueApiResponse.IssueType();
    issueType.setName("Bug");
    fields.setIssuetype(issueType);

    // Status
    JiraIssueApiResponse.Status status = new JiraIssueApiResponse.Status();
    status.setName("Open");
    fields.setStatus(status);

    // Priority
    JiraIssueApiResponse.Priority priority = new JiraIssueApiResponse.Priority();
    priority.setName("High");
    fields.setPriority(priority);

    // Resolution
    JiraIssueApiResponse.Resolution resolution = new JiraIssueApiResponse.Resolution();
    resolution.setName("Fixed");
    fields.setResolution(resolution);

    // Assignee
    JiraIssueApiResponse.User assignee = new JiraIssueApiResponse.User();
    assignee.setDisplayName("John Doe");
    assignee.setEmailAddress("john@example.com");
    fields.setAssignee(assignee);

    // Reporter
    JiraIssueApiResponse.User reporter = new JiraIssueApiResponse.User();
    reporter.setDisplayName("Jane Smith");
    reporter.setEmailAddress("jane@example.com");
    fields.setReporter(reporter);

    apiResponse.setFields(fields);
    return apiResponse;
  }
}

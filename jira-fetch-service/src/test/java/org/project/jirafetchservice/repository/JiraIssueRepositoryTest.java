package org.project.jirafetchservice.repository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.project.jirafetchservice.JiraFetchServiceApplication;
import org.project.jirafetchservice.entity.JiraIssueDbEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@DataR2dbcTest
@ActiveProfiles("test")
@ContextConfiguration(classes = JiraFetchServiceApplication.class)
class JiraIssueRepositoryTest {

  @Autowired private JiraIssueRepository jiraIssueRepository;

  private JiraIssueDbEntity testIssue1;
  private JiraIssueDbEntity testIssue2;
  private JiraIssueDbEntity testIssue3;

  @BeforeEach
  void setUp() {
    // Clean database before each test
    jiraIssueRepository.deleteAll().block();

    // Create test entities
    testIssue1 = createTestIssue("PROJ-123", "Open", "John Doe", "PROJ");
    testIssue2 = createTestIssue("PROJ-124", "Closed", "Jane Smith", "PROJ");
    testIssue3 = createTestIssue("OTHER-001", "Open", "John Doe", "OTHER");

    // Save test data
    jiraIssueRepository.saveAll(Arrays.asList(testIssue1, testIssue2, testIssue3)).blockLast();
  }

  @Test
  void save_shouldPersistEntity_whenValidEntity() {
    // Given
    JiraIssueDbEntity newIssue = createTestIssue("NEW-001", "In Progress", "Test User", "NEW");

    // When & Then
    StepVerifier.create(jiraIssueRepository.save(newIssue))
        .expectNextMatches(
            saved ->
                saved.getIssueKey().equals("NEW-001")
                    && saved.getStatus().equals("In Progress")
                    && saved.getAssignee().equals("Test User"))
        .verifyComplete();
  }

  @Test
  void findById_shouldReturnEntity_whenEntityExists() {
    // When & Then
    StepVerifier.create(jiraIssueRepository.findById(testIssue1.getIssueKey()))
        .expectNextMatches(
            found -> found.getIssueKey().equals("PROJ-123") && found.getStatus().equals("Open"))
        .verifyComplete();
  }

  @Test
  void findById_shouldReturnEmpty_whenEntityNotExists() {
    // When & Then
    StepVerifier.create(jiraIssueRepository.findById("NONEXISTENT-001")).verifyComplete();
  }

  @Test
  void findAll_shouldReturnAllEntities() {
    // When & Then
    StepVerifier.create(jiraIssueRepository.findAll()).expectNextCount(3).verifyComplete();
  }

  @Test
  void findByStatus_shouldReturnMatchingEntities_whenStatusExists() {
    // When & Then
    StepVerifier.create(jiraIssueRepository.findByStatus("Open"))
        .expectNextCount(2) // testIssue1 and testIssue3
        .verifyComplete();
  }

  @Test
  void findByStatus_shouldReturnEmpty_whenStatusNotExists() {
    // When & Then
    StepVerifier.create(jiraIssueRepository.findByStatus("NonExistentStatus")).verifyComplete();
  }

  @Test
  void findByAssignee_shouldReturnMatchingEntities_whenAssigneeExists() {
    // When & Then
    StepVerifier.create(jiraIssueRepository.findByAssignee("John Doe"))
        .expectNextCount(2) // testIssue1 and testIssue3
        .verifyComplete();
  }

  @Test
  void findByAssignee_shouldReturnEmpty_whenAssigneeNotExists() {
    // When & Then
    StepVerifier.create(jiraIssueRepository.findByAssignee("NonExistentUser")).verifyComplete();
  }

  @Test
  void findByIssueKey_shouldReturnMatchingEntity_whenIssueKeyExists() {
    // When & Then
    StepVerifier.create(jiraIssueRepository.findByIssueKey("PROJ-123"))
        .expectNextMatches(
            found -> found.getIssueKey().equals("PROJ-123") && found.getProjectKey().equals("PROJ"))
        .verifyComplete();
  }

  @Test
  void findByIssueKey_shouldReturnEmpty_whenIssueKeyNotExists() {
    // When & Then
    StepVerifier.create(jiraIssueRepository.findByIssueKey("NONEXISTENT-001")).verifyComplete();
  }

  @Test
  void findByProjectKey_shouldReturnMatchingEntities_whenProjectKeyExists() {
    // When & Then
    StepVerifier.create(jiraIssueRepository.findByProjectKey("PROJ"))
        .expectNextCount(2) // testIssue1 and testIssue2
        .verifyComplete();
  }

  @Test
  void findByProjectKey_shouldReturnEmpty_whenProjectKeyNotExists() {
    // When & Then
    StepVerifier.create(jiraIssueRepository.findByProjectKey("NONEXISTENT")).verifyComplete();
  }

  @Test
  void findByIssueKeyIn_shouldReturnMatchingEntities_whenIssueKeysExist() {
    // Given
    List<String> issueKeys = Arrays.asList("PROJ-123", "OTHER-001");

    // When & Then
    StepVerifier.create(jiraIssueRepository.findByIssueKeyIn(issueKeys))
        .expectNextCount(2)
        .verifyComplete();
  }

  @Test
  void findByIssueKeyIn_shouldReturnEmpty_whenNoIssueKeysMatch() {
    // Given
    List<String> nonExistentKeys = Arrays.asList("FAKE-001", "FAKE-002");

    // When & Then
    StepVerifier.create(jiraIssueRepository.findByIssueKeyIn(nonExistentKeys)).verifyComplete();
  }

  @Test
  void findByIssueKeyIn_shouldHandleEmptyList() {
    // Given
    List<String> emptyList = Arrays.asList();

    // When & Then
    StepVerifier.create(jiraIssueRepository.findByIssueKeyIn(emptyList)).verifyComplete();
  }

  @Test
  void findByProjectKeyIsNotNull_shouldReturnEntitiesWithProjectKey() {
    // When & Then
    StepVerifier.create(jiraIssueRepository.findByProjectKeyIsNotNull())
        .expectNextCount(3) // All test entities have projectKey
        .verifyComplete();
  }

  @Test
  void findByProjectKeyIsNotNull_shouldExcludeNullProjectKeys() {
    // Given - Create entity with null project key
    JiraIssueDbEntity issueWithNullProject = createTestIssue("NULL-001", "Open", "Test User", null);
    jiraIssueRepository.save(issueWithNullProject).block();

    // When & Then
    StepVerifier.create(jiraIssueRepository.findByProjectKeyIsNotNull())
        .expectNextCount(3) // Should still return only the 3 original entities
        .verifyComplete();
  }

  @Test
  void delete_shouldRemoveEntity_whenEntityExists() {
    // Given
    String issueKeyToDelete = testIssue1.getIssueKey();

    // When
    StepVerifier.create(jiraIssueRepository.deleteById(issueKeyToDelete)).verifyComplete();

    // Then - Verify entity is deleted
    StepVerifier.create(jiraIssueRepository.findById(issueKeyToDelete)).verifyComplete();

    // And verify count decreased
    StepVerifier.create(jiraIssueRepository.count()).expectNext(2L).verifyComplete();
  }

  @Test
  void deleteAll_shouldRemoveAllEntities() {
    // When
    StepVerifier.create(jiraIssueRepository.deleteAll()).verifyComplete();

    // Then
    StepVerifier.create(jiraIssueRepository.count()).expectNext(0L).verifyComplete();
  }

  @Test
  void count_shouldReturnCorrectCount() {
    // When & Then
    StepVerifier.create(jiraIssueRepository.count()).expectNext(3L).verifyComplete();
  }

  @Test
  void existsById_shouldReturnTrue_whenEntityExists() {
    // When & Then
    StepVerifier.create(jiraIssueRepository.existsById(testIssue1.getIssueKey()))
        .expectNext(true)
        .verifyComplete();
  }

  @Test
  void existsById_shouldReturnFalse_whenEntityNotExists() {
    // When & Then
    StepVerifier.create(jiraIssueRepository.existsById("NONEXISTENT-001"))
        .expectNext(false)
        .verifyComplete();
  }

  @Test
  void update_shouldModifyEntity_whenEntityExists() {
    // Given
    testIssue1.setStatus("Updated Status");
    testIssue1.setAssignee("Updated Assignee");
    testIssue1.setNewEntity(false); // Mark as existing entity for update operation

    // When
    StepVerifier.create(jiraIssueRepository.save(testIssue1))
        .expectNextMatches(
            updated ->
                updated.getStatus().equals("Updated Status")
                    && updated.getAssignee().equals("Updated Assignee"))
        .verifyComplete();

    // Then - Verify the update persisted
    StepVerifier.create(jiraIssueRepository.findById(testIssue1.getIssueKey()))
        .expectNextMatches(
            found ->
                found.getStatus().equals("Updated Status")
                    && found.getAssignee().equals("Updated Assignee"))
        .verifyComplete();
  }

  @Test
  void concurrentOperations_shouldHandleMultipleSimultaneousQueries() {
    // When - Execute multiple queries concurrently
    Flux<JiraIssueDbEntity> concurrentQueries =
        Flux.merge(
            jiraIssueRepository.findByStatus("Open"),
            jiraIssueRepository.findByAssignee("John Doe"),
            jiraIssueRepository.findByProjectKey("PROJ"));

    // Then
    StepVerifier.create(concurrentQueries)
        .expectNextCount(6) // 2 + 2 + 2 = 6 total results
        .verifyComplete();
  }

  @Test
  void saveAll_shouldPersistMultipleEntities() {
    // Given
    List<JiraIssueDbEntity> newIssues =
        Arrays.asList(
            createTestIssue("BATCH-001", "New", "User1", "BATCH"),
            createTestIssue("BATCH-002", "New", "User2", "BATCH"),
            createTestIssue("BATCH-003", "New", "User3", "BATCH"));

    // When & Then
    StepVerifier.create(jiraIssueRepository.saveAll(newIssues)).expectNextCount(3).verifyComplete();

    // Verify all were saved
    StepVerifier.create(jiraIssueRepository.findByProjectKey("BATCH"))
        .expectNextCount(3)
        .verifyComplete();
  }

  // Helper method to create test entities
  private JiraIssueDbEntity createTestIssue(
      String issueKey, String status, String assignee, String projectKey) {
    JiraIssueDbEntity entity = new JiraIssueDbEntity();
    entity.setId(issueKey); // Set ID to prevent NULL constraint violation
    entity.setIssueKey(issueKey);
    entity.setSummary("Test Summary for " + issueKey);
    entity.setIssueType("Bug");
    entity.setStatus(status);
    entity.setPriority("High");
    entity.setAssignee(assignee);
    entity.setReporter("Test Reporter");
    entity.setProjectKey(projectKey);
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
    entity.setQuotaPerProject("hello");
    return entity;
  }
}

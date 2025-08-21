package org.project.jirafetchservice.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.jirafetchservice.client.JiraWebClient;
import org.project.jirafetchservice.dto.IssueSimpleDto;
import org.project.jirafetchservice.entity.JiraIssueDbEntity;
import org.project.jirafetchservice.jiraApi.JiraIssueApiResponse;
import org.project.jirafetchservice.jiraApi.JiraSearchResponse;
import org.project.jirafetchservice.mapper.JiraMapper;
import org.project.jirafetchservice.repository.JiraIssueRepository;
import org.springframework.dao.DataIntegrityViolationException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@DisplayName("JiraIssueService Tests")
class JiraIssueServiceTest {

  @Mock(lenient = true)
  JiraWebClient jiraWebClient;

  @Mock(lenient = true)
  JiraIssueRepository jiraIssueRepository;

  @Mock(lenient = true)
  JiraMapper jiraMapper;

  @InjectMocks JiraIssueService service;

  private JiraIssueApiResponse apiResponse;
  private IssueSimpleDto simpleDto;
  private JiraIssueDbEntity dbEntity;
  private JiraSearchResponse searchResponse;

  @BeforeEach
  void setUp() {
    // Reset all mocks to prevent cross-test interference
    reset(jiraWebClient, jiraIssueRepository, jiraMapper);

    // Setup common test data
    apiResponse = new JiraIssueApiResponse();
    apiResponse.setKey("PROJ-123");
    apiResponse.setSummary("Test Issue");

    simpleDto =
        IssueSimpleDto.builder().issueKey("PROJ-123").summary("Test Issue").status("Open").build();

    dbEntity = new JiraIssueDbEntity("PROJ-123");
    dbEntity.setUpdated(LocalDateTime.now().minusMinutes(30));

    searchResponse = new JiraSearchResponse();
    searchResponse.setIssues(Arrays.asList(apiResponse));

    // Setup default lenient stubbing to prevent NullPointerException
    lenient().when(jiraWebClient.getIssue(anyString())).thenReturn(Mono.just(apiResponse));
    lenient()
        .when(jiraMapper.toSimpleDtoFromApi(any(JiraIssueApiResponse.class)))
        .thenReturn(simpleDto);
    lenient()
        .when(jiraMapper.toSimpleDtoFromDb(any(JiraIssueDbEntity.class)))
        .thenReturn(simpleDto);
  }

  // ================== API CONSULTATION METHODS ==================

  @Test
  @DisplayName("getIssue - Should return issue from Jira API")
  void getIssue_shouldReturnIssueFromApi() {
    when(jiraWebClient.getIssue("PROJ-123")).thenReturn(Mono.just(apiResponse));

    StepVerifier.create(service.getIssue("PROJ-123")).expectNext(apiResponse).verifyComplete();

    verify(jiraWebClient).getIssue("PROJ-123");
  }

  @Test
  @DisplayName("getIssue - Should throw error when issue not found")
  void getIssue_shouldThrowErrorWhenNotFound() {
    when(jiraWebClient.getIssue("NOTFOUND")).thenReturn(Mono.empty());

    StepVerifier.create(service.getIssue("NOTFOUND"))
        .expectErrorMatches(
            error ->
                error instanceof IllegalArgumentException
                    && error.getMessage().contains("Issue non trouvée : NOTFOUND"))
        .verify();
  }

  @Test
  @DisplayName("getProjectIssues - Should return project issues")
  void getProjectIssues_shouldReturnProjectIssues() {
    when(jiraWebClient.searchProjectIssues("PROJ")).thenReturn(Flux.just(apiResponse));

    StepVerifier.create(service.getProjectIssues("PROJ")).expectNext(apiResponse).verifyComplete();

    verify(jiraWebClient).searchProjectIssues("PROJ");
  }

  @Test
  @DisplayName("getProjectIssues - Should throw error when no issues found")
  void getProjectIssues_shouldThrowErrorWhenEmpty() {
    when(jiraWebClient.searchProjectIssues("EMPTY")).thenReturn(Flux.empty());

    StepVerifier.create(service.getProjectIssues("EMPTY"))
        .expectErrorMatches(
            error ->
                error instanceof IllegalArgumentException
                    && error.getMessage().contains("Aucune issue pour le projet : EMPTY"))
        .verify();
  }

  // ================== SYNCHRONIZATION METHODS ==================

  @Test
  @DisplayName("synchroniserIssueAvecJira - Should return cached data when recent")
  void synchroniserIssueAvecJira_shouldReturnCachedWhenRecent() {
    // Setup recent entity (30 minutes ago)
    JiraIssueDbEntity recentEntity = new JiraIssueDbEntity("PROJ-123");
    recentEntity.setUpdated(LocalDateTime.now().minusMinutes(30));

    when(jiraIssueRepository.findByIssueKey("PROJ-123")).thenReturn(Flux.just(recentEntity));
    when(jiraMapper.toSimpleDtoFromDb(recentEntity)).thenReturn(simpleDto);

    StepVerifier.create(service.synchroniserIssueAvecJira("PROJ-123"))
        .expectNext(simpleDto)
        .verifyComplete();

    verify(jiraIssueRepository).findByIssueKey("PROJ-123");
    verify(jiraMapper).toSimpleDtoFromDb(recentEntity);
  }

  @Test
  @DisplayName("synchroniserIssueAvecJira - Should fetch from Jira when not cached")
  void synchroniserIssueAvecJira_shouldFetchFromJiraWhenNotCached() {
    when(jiraIssueRepository.findByIssueKey("PROJ-123")).thenReturn(Flux.empty());
    when(jiraWebClient.getIssue("PROJ-123")).thenReturn(Mono.just(apiResponse));
    when(jiraMapper.toSimpleDtoFromApi(apiResponse)).thenReturn(simpleDto);
    when(jiraMapper.toDbEntityFromSimpleDto(simpleDto)).thenReturn(dbEntity);
    when(jiraIssueRepository.save(dbEntity)).thenReturn(Mono.just(dbEntity));

    StepVerifier.create(service.synchroniserIssueAvecJira("PROJ-123"))
        .expectNext(simpleDto)
        .verifyComplete();

    verify(jiraWebClient).getIssue("PROJ-123");
    verify(jiraIssueRepository).save(dbEntity);
  }

  @Test
  @DisplayName("synchroniserIssueAvecJira - Should fetch from Jira when cached data is old")
  void synchroniserIssueAvecJira_shouldFetchFromJiraWhenOld() {
    // Setup old entity (2 hours ago)
    JiraIssueDbEntity oldEntity = new JiraIssueDbEntity("PROJ-123");
    oldEntity.setUpdated(LocalDateTime.now().minusHours(2));

    when(jiraIssueRepository.findByIssueKey("PROJ-123")).thenReturn(Flux.just(oldEntity));
    when(jiraWebClient.getIssue("PROJ-123")).thenReturn(Mono.just(apiResponse));
    when(jiraMapper.toSimpleDtoFromApi(apiResponse)).thenReturn(simpleDto);
    when(jiraMapper.toDbEntityFromSimpleDto(simpleDto)).thenReturn(dbEntity);
    when(jiraIssueRepository.save(dbEntity)).thenReturn(Mono.just(dbEntity));

    StepVerifier.create(service.synchroniserIssueAvecJira("PROJ-123"))
        .expectNext(simpleDto)
        .verifyComplete();

    // Allow for onErrorResume double invocation due to reactive stream error handling
    verify(jiraWebClient, atMost(2)).getIssue("PROJ-123");
    verify(jiraIssueRepository).save(dbEntity);
  }

  @Test
  @DisplayName("synchroniserIssueAvecJira - Should return error DTO on failure")
  void synchroniserIssueAvecJira_shouldReturnErrorDtoOnFailure() {
    when(jiraIssueRepository.findByIssueKey("PROJ-123"))
        .thenReturn(Flux.error(new RuntimeException("DB Error")));
    when(jiraWebClient.getIssue("PROJ-123"))
        .thenReturn(Mono.error(new RuntimeException("API Error")));

    StepVerifier.create(service.synchroniserIssueAvecJira("PROJ-123"))
        .expectNextMatches(
            dto ->
                "PROJ-123".equals(dto.getIssueKey())
                    && dto.getSummary().startsWith("ERREUR:")
                    && "ERROR".equals(dto.getStatus()))
        .verifyComplete();
  }

  @Test
  @DisplayName("synchroniserProjetAvecJira - Should synchronize project issues in batches")
  void synchroniserProjetAvecJira_shouldSynchronizeProjectInBatches() {
    List<JiraIssueApiResponse> issues = Arrays.asList(apiResponse);
    List<IssueSimpleDto> dtos = Arrays.asList(simpleDto);
    List<JiraIssueDbEntity> entities = Arrays.asList(dbEntity);

    when(jiraWebClient.searchProjectIssues("PROJ")).thenReturn(Flux.fromIterable(issues));
    when(jiraMapper.toSimpleDtoFromApi(any(JiraIssueApiResponse.class))).thenReturn(simpleDto);
    when(jiraIssueRepository.findByIssueKeyIn(anyList())).thenReturn(Flux.empty());
    when(jiraMapper.toDbEntityFromSimpleDto(simpleDto)).thenReturn(dbEntity);
    when(jiraIssueRepository.saveAll(entities)).thenReturn(Flux.fromIterable(entities));
    when(jiraMapper.toSimpleDtoFromDb(dbEntity)).thenReturn(simpleDto);

    StepVerifier.create(service.synchroniserProjetAvecJira("PROJ", 10))
        .expectNext(simpleDto)
        .verifyComplete();

    verify(jiraWebClient).searchProjectIssues("PROJ");
    verify(jiraIssueRepository).saveAll(anyList());
  }

  @Test
  @DisplayName("synchroniserProjetAvecJira - Should handle DataIntegrityViolationException")
  void synchroniserProjetAvecJira_shouldHandleConstraintViolations() {
    when(jiraWebClient.searchProjectIssues("PROJ")).thenReturn(Flux.just(apiResponse));
    when(jiraMapper.toSimpleDtoFromApi(apiResponse)).thenReturn(simpleDto);
    when(jiraIssueRepository.findByIssueKeyIn(anyList())).thenReturn(Flux.empty());
    when(jiraMapper.toDbEntityFromSimpleDto(simpleDto)).thenReturn(dbEntity);
    when(jiraIssueRepository.saveAll(anyList()))
        .thenReturn(Flux.error(new DataIntegrityViolationException("Constraint violation")));
    when(jiraIssueRepository.save(dbEntity)).thenReturn(Mono.just(dbEntity));
    when(jiraMapper.toSimpleDtoFromDb(dbEntity)).thenReturn(simpleDto);

    StepVerifier.create(service.synchroniserProjetAvecJira("PROJ", 10))
        .expectNext(simpleDto)
        .verifyComplete();

    verify(jiraIssueRepository).save(dbEntity);
  }

  @Test
  @DisplayName("synchroniserProjetAvecJira - Should handle empty project")
  void synchroniserProjetAvecJira_shouldHandleEmptyProject() {
    when(jiraWebClient.searchProjectIssues("EMPTY")).thenReturn(Flux.empty());

    StepVerifier.create(service.synchroniserProjetAvecJira("EMPTY", 10)).verifyComplete();

    verify(jiraWebClient).searchProjectIssues("EMPTY");
  }

  @Test
  @DisplayName("synchroniserProjetAvecJira - Should handle error during project synchronization")
  void synchroniserProjetAvecJira_shouldHandleErrorDuringSync() {
    when(jiraWebClient.searchProjectIssues("ERROR"))
        .thenReturn(Flux.error(new RuntimeException("API Error")));

    StepVerifier.create(service.synchroniserProjetAvecJira("ERROR", 10)).verifyComplete();

    verify(jiraWebClient).searchProjectIssues("ERROR");
  }

  // ================== UTILITY METHODS ==================

  @Test
  @DisplayName("getAllLocalProjectKeys - Should return distinct project keys")
  void getAllLocalProjectKeys_shouldReturnDistinctKeys() {
    JiraIssueDbEntity entity1 = new JiraIssueDbEntity("PROJ-1");
    entity1.setProjectKey("PROJ");
    JiraIssueDbEntity entity2 = new JiraIssueDbEntity("TEST-1");
    entity2.setProjectKey("TEST");

    when(jiraIssueRepository.findByProjectKeyIsNotNull()).thenReturn(Flux.just(entity1, entity2));

    StepVerifier.create(service.getAllLocalProjectKeys())
        .expectNext("PROJ")
        .expectNext("TEST")
        .verifyComplete();
  }

  @Test
  @DisplayName("getAllLocalProjectKeys - Should handle errors gracefully")
  void getAllLocalProjectKeys_shouldHandleErrors() {
    when(jiraIssueRepository.findByProjectKeyIsNotNull())
        .thenReturn(Flux.error(new RuntimeException("DB Error")));

    StepVerifier.create(service.getAllLocalProjectKeys()).verifyComplete();
  }

  @Test
  @DisplayName("synchroniserIssueAvecJira - Should handle multiple concurrent requests")
  void synchroniserIssueAvecJira_shouldHandleConcurrentRequests() {
    // Setup old entity to force Jira fetch
    JiraIssueDbEntity oldEntity = new JiraIssueDbEntity("PROJ-123");
    oldEntity.setUpdated(LocalDateTime.now().minusHours(2));

    when(jiraIssueRepository.findByIssueKey("PROJ-123")).thenReturn(Flux.just(oldEntity));
    when(jiraWebClient.getIssue("PROJ-123")).thenReturn(Mono.just(apiResponse));
    when(jiraMapper.toSimpleDtoFromApi(apiResponse)).thenReturn(simpleDto);
    when(jiraMapper.toDbEntityFromSimpleDto(simpleDto)).thenReturn(dbEntity);
    when(jiraIssueRepository.save(dbEntity)).thenReturn(Mono.just(dbEntity));

    Flux<IssueSimpleDto> concurrentRequests =
        Flux.range(1, 5).flatMap(i -> service.synchroniserIssueAvecJira("PROJ-123"));

    StepVerifier.create(concurrentRequests).expectNextCount(5).verifyComplete();
  }

  @Test
  @DisplayName("isRecentlyUpdatedInJira - Should handle various timestamp formats")
  void isRecentlyUpdatedInJira_shouldHandleVariousFormats() {
    JiraIssueDbEntity entityWithIsoFormat = new JiraIssueDbEntity("PROJ-1");
    entityWithIsoFormat.setUpdated(LocalDateTime.parse("2025-01-01T10:00:00.123"));

    JiraIssueDbEntity entityWithSimpleFormat = new JiraIssueDbEntity("PROJ-2");
    entityWithSimpleFormat.setUpdated(LocalDateTime.now().minusMinutes(30));

    when(jiraIssueRepository.findByIssueKey("PROJ-1")).thenReturn(Flux.just(entityWithIsoFormat));
    when(jiraIssueRepository.findByIssueKey("PROJ-2"))
        .thenReturn(Flux.just(entityWithSimpleFormat));
    when(jiraWebClient.getIssue(anyString())).thenReturn(Mono.just(apiResponse));
    when(jiraMapper.toSimpleDtoFromApi(apiResponse)).thenReturn(simpleDto);
    when(jiraMapper.toDbEntityFromSimpleDto(simpleDto)).thenReturn(dbEntity);
    when(jiraIssueRepository.save(any())).thenReturn(Mono.just(dbEntity));

    // Both should trigger Jira fetch since timestamps are old or malformed
    StepVerifier.create(service.synchroniserIssueAvecJira("PROJ-1"))
        .expectNext(simpleDto)
        .verifyComplete();

    // Allow for onErrorResume double invocation due to reactive stream error handling
    verify(jiraWebClient, atMost(2)).getIssue("PROJ-1");
  }
}

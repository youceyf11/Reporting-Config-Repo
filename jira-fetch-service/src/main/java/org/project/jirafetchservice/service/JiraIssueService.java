package org.project.jirafetchservice.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.project.issueevents.events.IssueUpsertedEvent;
import org.project.jirafetchservice.client.JiraWebClient;
import org.project.jirafetchservice.dto.IssueSimpleDto;
import org.project.jirafetchservice.entity.JiraIssueDbEntity;
import org.project.jirafetchservice.jiraApi.JiraIssueApiResponse;
import org.project.jirafetchservice.kafka.JiraIssueEventProducer;
import org.project.jirafetchservice.mapper.JiraMapper;
import org.project.jirafetchservice.repository.JiraIssueRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class JiraIssueService {

  private final JiraWebClient jiraWebClient;
  private final JiraIssueRepository jiraIssueRepository;
  private final JiraMapper jiraMapper;
  private final JiraIssueEventProducer eventProducer;

  public JiraIssueService(
      JiraWebClient jiraWebClient,
      JiraIssueRepository jiraIssueRepository,
      JiraMapper jiraMapper,
      JiraIssueEventProducer eventProducer) {
    this.jiraWebClient = jiraWebClient;
    this.jiraIssueRepository = jiraIssueRepository;
    this.jiraMapper = jiraMapper;
    this.eventProducer = eventProducer;
  }

  // ================== MÉTHODES DE CONSULTATION (API uniquement) ==================

  public Mono<JiraIssueApiResponse> getIssue(String issueKey) {
    return jiraWebClient
        .getIssue(issueKey)
        .switchIfEmpty(Mono.error(new IllegalArgumentException("Issue non trouvée : " + issueKey)));
  }

  public Flux<JiraIssueApiResponse> getProjectIssues(String projectKey) {
    return jiraWebClient
        .searchProjectIssues(projectKey)
        .switchIfEmpty(
            Flux.error(
                new IllegalArgumentException("Aucune issue pour le projet : " + projectKey)));
  }

  public Flux<JiraIssueApiResponse> getIssuesAssignedTo(String assigneeEmail) {
    String jql = "assignee = '" + assigneeEmail + "'";
    return searchIssues(jql);
  }

  public Flux<JiraIssueApiResponse> searchIssues(String jql) {
    return jiraWebClient
        .searchIssues(jql)
        .flatMapMany(response -> Flux.fromIterable(response.getIssues()));
  }

  // ================== MÉTHODES DE SYNCHRONISATION OPTIMISÉES ==================

  public Mono<IssueSimpleDto> synchroniserIssueAvecJira(String issueKey) {
    return jiraIssueRepository
        .findByIssueKey(issueKey)
        .next()
        .flatMap(
            entity -> {
              System.out.println(
                  "DEBUG: Found existing entity for "
                      + issueKey
                      + ", updated="
                      + entity.getUpdated());
              if (entity.getUpdated() != null && isRecentlyUpdatedInJira(entity.getUpdated())) {
                System.out.println("DEBUG: Using cached data for " + issueKey);
                try {
                  IssueSimpleDto dto = jiraMapper.toSimpleDtoFromDb(entity);
                  System.out.println(
                      "DEBUG: Mapper returned DTO: " + (dto != null ? dto.getIssueKey() : "NULL"));
                  return Mono.just(dto);
                } catch (Exception e) {
                  System.err.println("DEBUG: Mapper threw exception: " + e.getMessage());
                  e.printStackTrace();
                  throw e;
                }
              }
              System.out.println("DEBUG: Entity not recent, fetching from Jira for " + issueKey);
              return synchroniserDepuisJira(issueKey);
            })
        .switchIfEmpty(
            Mono.fromRunnable(
                    () ->
                        System.out.println(
                            "DEBUG: No entity found, fetching from Jira for " + issueKey))
                .then(synchroniserDepuisJira(issueKey)))
        .doOnError(
            error -> {
              System.err.println(
                  "DEBUG: doOnError triggered for " + issueKey + ": " + error.getMessage());
              error.printStackTrace();
            })
        .onErrorResume(
            error -> {
              System.err.println(
                  "DEBUG: onErrorResume triggered for " + issueKey + ": " + error.getMessage());
              // Fallback : essayer de récupérer depuis Jira directement
              return jiraWebClient
                  .getIssue(issueKey)
                  .map(jiraMapper::toSimpleDtoFromApi)
                  .onErrorReturn(createErrorDto(issueKey, error.getMessage()));
            });
  }

  private IssueSimpleDto createErrorDto(String issueKey, String errorMessage) {
    IssueSimpleDto errorDto = new IssueSimpleDto();
    errorDto.setIssueKey(issueKey);
    errorDto.setSummary("ERREUR: " + errorMessage);
    errorDto.setStatus("ERROR");
    return errorDto;
  }

  private Mono<IssueSimpleDto> synchroniserDepuisJira(String issueKey) {
    return jiraWebClient
        .getIssue(issueKey)
        .map(jiraMapper::toSimpleDtoFromApi)
        .flatMap(
            issueDto -> {
              JiraIssueDbEntity entity = jiraMapper.toDbEntityFromSimpleDto(issueDto);
              return jiraIssueRepository
                  .save(entity)
                  .map(savedEntity -> issueDto)
                  .flatMap(
                      dto -> publishIssueEvent(dto).thenReturn(dto)); // Publish event after save
            })
        .doOnError(
            error ->
                System.err.println(
                    "Erreur lors de la synchronisation depuis Jira: " + error.getMessage()));
  }

  public Flux<IssueSimpleDto> synchroniserProjetAvecJira(String projectKey, Integer batchSize) {
    AtomicInteger processedCount = new AtomicInteger(0);

    return jiraWebClient
        .searchProjectIssues(projectKey)
        .doOnNext(issue -> System.out.println("Traitement de l'issue: " + issue.getKey()))
        .map(jiraMapper::toSimpleDtoFromApi)
        .buffer(batchSize)
        .concatMap(
            issueBatch -> {
              System.out.println("Traitement d'un batch de " + issueBatch.size() + " issues");

              return filtrerIssuesObsoletes(issueBatch)
                  .collectList()
                  .flatMapMany(
                      issuesToSync -> {
                        if (issuesToSync.isEmpty()) {
                          System.out.println("Aucune issue à synchroniser dans ce batch");
                          return Flux.empty();
                        }

                        System.out.println("Synchronisation de " + issuesToSync.size() + " issues");
                        List<JiraIssueDbEntity> entities =
                            issuesToSync.stream().map(jiraMapper::toDbEntityFromSimpleDto).toList();

                        return jiraIssueRepository
                            .saveAll(entities)
                            .onErrorResume(
                                org.springframework.dao.DataIntegrityViolationException.class,
                                ex -> {
                                  // Handle constraint violations by trying individual saves
                                  return Flux.fromIterable(entities)
                                      .flatMap(
                                          entity ->
                                              jiraIssueRepository
                                                  .save(entity)
                                                  .onErrorResume(
                                                      constraintEx -> {
                                                        // Log the error instead of silently
                                                        // ignoring
                                                        System.err.println(
                                                            "Failed to save entity "
                                                                + entity.getId()
                                                                + ": "
                                                                + constraintEx.getMessage());
                                                        return Mono.empty();
                                                      }));
                                })
                            .map(jiraMapper::toSimpleDtoFromDb)
                            .flatMap(
                                dto ->
                                    publishIssueEvent(dto)
                                        .thenReturn(dto)) // Publish event after save
                            .doOnNext(dto -> processedCount.incrementAndGet());
                      })
                  .onErrorResume(
                      error -> {
                        System.err.println(
                            "Erreur lors du traitement du batch: " + error.getMessage());
                        error.printStackTrace();
                        return Flux.empty();
                      });
            })
        .doOnComplete(
            () ->
                System.out.println(
                    "Synchronisation terminée. Total traité: " + processedCount.get()))
        .doOnError(
            error ->
                System.err.println(
                    "Erreur lors de la synchronisation du projet: " + error.getMessage()))
        .onErrorResume(
            error -> {
              System.err.println("Erreur générale de synchronisation: " + error.getMessage());
              return Flux.empty();
            });
  }

  public Flux<IssueSimpleDto> synchroniserSearchAvecJira(String jql) {
    return jiraWebClient
        .searchIssues(jql)
        .flatMapMany(response -> Flux.fromIterable(response.getIssues()))
        .map(jiraMapper::toSimpleDtoFromApi)
        .flatMap(
            issueDto -> {
              JiraIssueDbEntity entity = jiraMapper.toDbEntityFromSimpleDto(issueDto);
              return jiraIssueRepository
                  .save(entity)
                  .map(savedEntity -> issueDto)
                  .flatMap(
                      dto -> publishIssueEvent(dto).thenReturn(dto)); // Publish event after save
            })
        .doOnError(
            error ->
                System.err.println(
                    "Erreur lors de la synchronisation de recherche: " + error.getMessage()));
  }

  // Overloaded method for LocalDateTime objects (from database)
  private boolean isRecentlyUpdatedInJira(LocalDateTime updated) {
    if (updated == null) {
      return false;
    }
    LocalDateTime threshold = LocalDateTime.now().minusHours(1);
    boolean isRecent = updated.isAfter(threshold);
    System.out.println(
        "DEBUG isRecentlyUpdatedInJira: updated="
            + updated
            + ", threshold="
            + threshold
            + ", isRecent="
            + isRecent);
    return isRecent;
  }

  // Filtre les issues qui ne nécessitent pas de mise à jour
  private Flux<IssueSimpleDto> filtrerIssuesObsoletes(List<IssueSimpleDto> issues) {
    List<String> issueKeys = issues.stream().map(IssueSimpleDto::getIssueKey).toList();

    return jiraIssueRepository
        .findByIssueKeyIn(issueKeys)
        .collectList()
        .flatMapMany(
            existingEntities -> {
              List<String> recentKeys =
                  existingEntities.stream()
                      .filter(
                          entity ->
                              entity.getUpdated() != null
                                  && isRecentlyUpdatedInJira(entity.getUpdated()))
                      .map(JiraIssueDbEntity::getIssueKey)
                      .toList();

              return Flux.fromIterable(issues)
                  .filter(issue -> !recentKeys.contains(issue.getIssueKey()));
            });
  }

  public Flux<String> getAllLocalProjectKeys() {
    return jiraIssueRepository
        .findByProjectKeyIsNotNull()
        .map(JiraIssueDbEntity::getProjectKey)
        .distinct()
        .onErrorResume(
            error -> {
              System.err.println(
                  "Erreur lors de la récupération des projets locaux: " + error.getMessage());
              return Flux.empty();
            });
  }

  public Mono<String> getProjectKeyFromIssue(String issueKey) {
    if (issueKey == null || !issueKey.contains("-")) {
      return Mono.error(new IllegalArgumentException("Format d'issue key invalide"));
    }
    return Mono.just(issueKey.split("-")[0]);
  }

  // ================== MÉTHODES DB LOCALE ==================

  public Mono<IssueSimpleDto> recupererIssueLocale(String issueKey) {
    return jiraIssueRepository
        .findByIssueKey(issueKey)
        .next()
        .map(jiraMapper::toSimpleDtoFromDb)
        .switchIfEmpty(
            Mono.error(new IllegalArgumentException("Issue non trouvée en base : " + issueKey)));
  }

  /** Publishes an IssueUpsertedEvent to Kafka after an issue is saved to the database */
  private Mono<Void> publishIssueEvent(IssueSimpleDto issueDto) {
    try {
      IssueUpsertedEvent event =
          new IssueUpsertedEvent(
              issueDto.getProjectKey(),
              issueDto.getIssueKey(),
              issueDto.getAssignee(),
              issueDto.getTimeSpentSeconds(),
              issueDto.getResolved() != null
                  ? issueDto.getResolved().toInstant(ZoneOffset.UTC)
                  : null);
      return eventProducer.publish(event);
    } catch (Exception e) {
      // Log error but don't fail the main flow
      System.err.println(
          "Failed to publish event for issue: " + issueDto.getIssueKey() + " - " + e.getMessage());
      return Mono.empty();
    }
  }
}

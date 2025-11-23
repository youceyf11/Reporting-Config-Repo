package org.project.jirafetchservice.service;

import org.project.issueevents.events.IssueUpsertedEvent;
import org.project.jirafetchservice.client.JiraWebClient;
import org.project.jirafetchservice.dto.IssueSimpleDto;
import org.project.jirafetchservice.entity.JiraIssueDbEntity;
import org.project.jirafetchservice.exception.JiraSynchronizationException;
import org.project.jirafetchservice.jirapi.JiraIssueApiResponse;
import org.project.jirafetchservice.jirapi.JiraSearchResponse;
import org.project.jirafetchservice.kafka.JiraIssueEventProducer;
import org.project.jirafetchservice.mapper.JiraMapper;
import org.project.jirafetchservice.repository.JiraIssueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for managing Jira issues synchronization between Jira API and local database.
 * Handles fetching, caching, and event publishing for Jira issues.
 */
@Service
public class JiraIssueService {

    private static final Logger logger = LoggerFactory.getLogger(JiraIssueService.class);
    private static final int CACHE_VALIDITY_HOURS = 1;
    private static final int DEFAULT_BATCH_SIZE = 50;

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

    // ================== API QUERY METHODS (READ ONLY) ==================

    public JiraIssueApiResponse getIssue(String issueKey) {
        try {
            JiraIssueApiResponse response = jiraWebClient.getIssue(issueKey);
            if (response == null) {
                logger.warn("Issue {} not found in Jira API", issueKey);
                return null;
            }
            return response;
        } catch (Exception e) {
            logger.error("Error fetching issue {}: {}", issueKey, e.getMessage());
            throw new RuntimeException("Failed to fetch issue " + issueKey, e);
        }
    }

    public List<JiraIssueApiResponse> getProjectIssues(String projectKey) {
        try {
            List<JiraIssueApiResponse> issues = jiraWebClient.searchProjectIssues(projectKey);
            if (issues == null || issues.isEmpty()) {
                logger.warn("No issues found for project: {}", projectKey);
                return new ArrayList<>();
            }
            return issues;
        } catch (Exception e) {
            logger.error("Error fetching project issues for {}: {}", projectKey, e.getMessage());
            throw new RuntimeException("Failed to fetch issues for project: " + projectKey, e);
        }
    }

    public List<JiraIssueApiResponse> getIssuesAssignedTo(String assigneeEmail) {
        try {
            String jql = String.format("assignee = '%s'", assigneeEmail);
            return searchIssues(jql);
        } catch (Exception e) {
            logger.error("Error fetching issues for assignee {}: {}", assigneeEmail, e.getMessage());
            throw new RuntimeException("Failed to fetch issues for assignee: " + assigneeEmail, e);
        }
    }

    public List<JiraIssueApiResponse> searchIssues(String jql) {
        try {
            JiraSearchResponse response = jiraWebClient.searchIssues(jql);
            if (response == null || response.getIssues() == null) {
                return new ArrayList<>();
            }
            return response.getIssues();
        } catch (Exception e) {
            logger.error("Error executing JQL search [{}]: {}", jql, e.getMessage());
            throw new RuntimeException("Failed to execute JQL search", e);
        }
    }

    // ================== OPTIMIZED SYNCHRONIZATION METHODS (READ + WRITE) ==================

    @Transactional
    public IssueSimpleDto synchronizeIssueWithJira(String issueKey) {
        try {
            Optional<JiraIssueDbEntity> existingEntity = jiraIssueRepository.findByIssueKey(issueKey);

            if (existingEntity.isPresent()) {
                JiraIssueDbEntity entity = existingEntity.get();
                if (isCacheValid(entity.getUpdated())) {
                    logger.debug("Using cached data for {}", issueKey);
                    return jiraMapper.toSimpleDtoFromDb(entity);
                }
                logger.debug("Cache expired for {}, fetching from Jira", issueKey);
            } else {
                logger.debug("No local entity found for {}, fetching from Jira", issueKey);
            }

            return fetchAndSaveFromJira(issueKey);

        } catch (Exception error) {
            logger.error("Error synchronizing issue {}: {}", issueKey, error.getMessage(), error);
            // Throwing exception ensures the Controller returns 500 Error instead of 200 OK with empty body
            throw new JiraSynchronizationException("Failed to synchronize issue " + issueKey);
        }
    }

    @Transactional
    public List<IssueSimpleDto> synchronizeProjectWithJira(String projectKey, Integer batchSize) {
        int effectiveBatchSize = (batchSize != null && batchSize > 0) ? batchSize : DEFAULT_BATCH_SIZE;
        List<IssueSimpleDto> result = new ArrayList<>();

        try {
            logger.info("Starting synchronization for project: {}", projectKey);

            // 1. Fetch raw data from API
            List<JiraIssueApiResponse> apiIssues = jiraWebClient.searchProjectIssues(projectKey);

            if (apiIssues == null || apiIssues.isEmpty()) {
                logger.warn("No issues returned from Jira for project {}", projectKey);
                return result;
            }

            logger.info("Fetched {} issues from API for project {}", apiIssues.size(), projectKey);

            // 2. Map DIRECTLY to Entities (Preserving all data)
            List<JiraIssueDbEntity> allEntities = apiIssues.stream()
                    .map(jiraMapper::toDbEntityFromApi)
                    .collect(Collectors.toList());

            // 3. Process in batches
            for (int i = 0; i < allEntities.size(); i += effectiveBatchSize) {
                int endIndex = Math.min(i + effectiveBatchSize, allEntities.size());
                List<JiraIssueDbEntity> batch = allEntities.subList(i, endIndex);

                logger.debug("Processing batch of {} issues (from {} to {})", batch.size(), i, endIndex);

                // 4. Filter Entities based on existing DB state
                List<JiraIssueDbEntity> entitiesToSave = filterOutdatedEntities(batch);

                if (entitiesToSave.isEmpty()) {
                    logger.debug("All issues in this batch are up-to-date.");
                    continue;
                }

                // 5. Save Entities and convert to DTOs for return
                result.addAll(saveBatchAndPublishEvents(entitiesToSave));
            }

            logger.info("Synchronization completed for project {}. Total processed: {}", projectKey, result.size());
            return result;

        } catch (Exception error) {
            logger.error("Fatal error synchronizing project {}: {}", projectKey, error.getMessage());
            throw new JiraSynchronizationException("Failed to synchronize project: " + projectKey);
        }
    }

    @Transactional
    public List<IssueSimpleDto> synchronizeSearchWithJira(String jql) {
        List<IssueSimpleDto> result = new ArrayList<>();

        try {
            logger.info("Starting synchronization for JQL: {}", jql);
            JiraSearchResponse response = jiraWebClient.searchIssues(jql);

            if (response == null || response.getIssues() == null) {
                return result;
            }

            List<JiraIssueApiResponse> apiIssues = response.getIssues();

            for (JiraIssueApiResponse apiResponse : apiIssues) {
                try {
                    // Map API -> Entity (Full Data)
                    JiraIssueDbEntity entity = jiraMapper.toDbEntityFromApi(apiResponse);

                    // Save to DB
                    JiraIssueDbEntity savedEntity = jiraIssueRepository.save(entity);

                    // Convert to DTO for output/kafka
                    IssueSimpleDto issueDto = jiraMapper.toSimpleDtoFromDb(savedEntity);

                    publishIssueEvent(issueDto);
                    result.add(issueDto);
                } catch (Exception innerEx) {
                    logger.error("Failed to save specific issue from JQL search: {}", apiResponse.getKey(), innerEx);
                    // Continue processing other issues even if one fails
                }
            }

            logger.info("Synchronized {} issues from JQL search", result.size());
            return result;

        } catch (Exception e) {
            logger.error("Error synchronizing search results: {}", e.getMessage(), e);
            throw new JiraSynchronizationException("Failed to sync search results");
        }
    }

    // ================== LOCAL DATABASE METHODS ==================

    public IssueSimpleDto getLocalIssue(String issueKey) {
        try {
            return jiraIssueRepository.findByIssueKey(issueKey)
                    .map(jiraMapper::toSimpleDtoFromDb)
                    .orElse(null); // Returning null allows 404 handling in controller
        } catch (Exception e) {
            logger.error("Database error fetching local issue {}: {}", issueKey, e.getMessage());
            throw new RuntimeException("Database error fetching issue", e);
        }
    }

    public List<String> getAllLocalProjectKeys() {
        try {
            return jiraIssueRepository.findByProjectKeyIsNotNull().stream()
                    .map(JiraIssueDbEntity::getProjectKey)
                    .distinct()
                    .collect(Collectors.toList());
        } catch (Exception error) {
            logger.error("Error retrieving local project keys: {}", error.getMessage(), error);
            return new ArrayList<>();
        }
    }

    public String getProjectKeyFromIssue(String issueKey) {
        if (issueKey == null || !issueKey.contains("-")) {
            throw new IllegalArgumentException("Invalid issue key format: " + issueKey);
        }
        return issueKey.split("-")[0];
    }

    // ================== PRIVATE HELPER METHODS ==================

    private boolean isCacheValid(LocalDateTime updated) {
        if (updated == null) return false;
        LocalDateTime threshold = LocalDateTime.now().minusHours(CACHE_VALIDITY_HOURS);
        return updated.isAfter(threshold);
    }

    /**
     * Fetches from API, Maps to Entity, Saves to DB, Returns DTO.
     */
    private IssueSimpleDto fetchAndSaveFromJira(String issueKey) {
        try {
            // 1. Get API Response
            JiraIssueApiResponse apiResponse = jiraWebClient.getIssue(issueKey);
            if (apiResponse == null) {
                throw new IllegalArgumentException("Issue not found in Jira: " + issueKey);
            }

            // 2. Map to Entity (Full Save)
            JiraIssueDbEntity entity = jiraMapper.toDbEntityFromApi(apiResponse);

            // 3. Save
            JiraIssueDbEntity savedEntity = jiraIssueRepository.save(entity);

            // 4. Convert to DTO
            IssueSimpleDto issueDto = jiraMapper.toSimpleDtoFromDb(savedEntity);

            // 5. Publish
            publishIssueEvent(issueDto);

            logger.debug("Successfully synchronized issue {} from Jira", issueKey);
            return issueDto;

        } catch (IllegalArgumentException e) {
            throw e; // Rethrow validation errors
        } catch (Exception error) {
            logger.error("Failed to fetch and save issue {}", issueKey, error);
            throw new JiraSynchronizationException("Failed to fetch and save issue " + issueKey);
        }
    }

    /**
     * Filters Entities (not DTOs) to find which ones need updating.
     */
    private List<JiraIssueDbEntity> filterOutdatedEntities(List<JiraIssueDbEntity> entities) {
        try {
            List<String> keys = entities.stream().map(JiraIssueDbEntity::getIssueKey).toList();
            List<JiraIssueDbEntity> existing = jiraIssueRepository.findByIssueKeyIn(keys);

            List<String> upToDateKeys = existing.stream()
                    .filter(e -> isCacheValid(e.getUpdated()))
                    .map(JiraIssueDbEntity::getIssueKey)
                    .toList();

            return entities.stream()
                    .filter(e -> !upToDateKeys.contains(e.getIssueKey()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error filtering outdated entities, proceeding to save all", e);
            return entities; // Fail safe: return all to ensure data integrity
        }
    }

    /**
     * Saves Entities and converts the result to DTOs for return/Kafka.
     */
    private List<IssueSimpleDto> saveBatchAndPublishEvents(List<JiraIssueDbEntity> entitiesToSave) {
        List<IssueSimpleDto> result = new ArrayList<>();

        try {
            // Try Batch Save first
            List<JiraIssueDbEntity> savedEntities = jiraIssueRepository.saveAll(entitiesToSave);

            for (JiraIssueDbEntity savedEntity : savedEntities) {
                IssueSimpleDto dto = jiraMapper.toSimpleDtoFromDb(savedEntity);
                publishIssueEvent(dto);
                result.add(dto);
            }

        } catch (Exception batchError) {
            logger.warn("Batch save failed ({} issues), falling back to individual saves. Error: {}",
                    entitiesToSave.size(), batchError.getMessage());

            // Fallback: Save one by one
            for (JiraIssueDbEntity entity : entitiesToSave) {
                try {
                    JiraIssueDbEntity savedEntity = jiraIssueRepository.save(entity);
                    IssueSimpleDto dto = jiraMapper.toSimpleDtoFromDb(savedEntity);
                    publishIssueEvent(dto);
                    result.add(dto);
                } catch (Exception individualError) {
                    logger.error("Failed to save entity for issue {}: {}", entity.getIssueKey(), individualError.getMessage());
                }
            }
        }
        return result;
    }

    private void publishIssueEvent(IssueSimpleDto dto) {
        try {
            // Ensure types match IssueUpsertedEvent constructor exactly
            IssueUpsertedEvent event = new IssueUpsertedEvent(
                    dto.getProjectKey(),                // String projectKey
                    dto.getIssueKey(),                  // String issueKey
                    dto.getAssignee(),                  // String assignee
                    dto.getTimeSpentSeconds(),          // Long timeSpentSeconds
                    dto.getStoryPoints(),               // Double storyPoints
                    dto.getResolved() != null           // Instant resolvedAt
                            ? dto.getResolved().toInstant(ZoneOffset.UTC)
                            : null
            );
            eventProducer.publish(event);
        } catch (Exception e) {
            logger.error("Kafka publish failed for {}: {}", dto.getIssueKey(), e.getMessage());
        }
    }
}
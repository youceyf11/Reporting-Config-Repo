package org.project.excelservice.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.excelservice.entity.Issue;
import org.project.excelservice.repository.IssueRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/** Service for syncing issues with an Excel file. */
@Service
@Slf4j
@RequiredArgsConstructor
public class ExcelSyncService {

  private final ExcelFileWriter writer;
  private final CheckpointStore checkpointService;
  private final IssueRepository issueRepository;

  /**
   * Syncs issues for the given project key with the Excel file.
   *
   * @param projectKey the project key
   * @return a Mono with a success message or an error
   */
  public Mono<String> sync(String projectKey) {
    // Retrieve the last processed timestamp (ISO-8601 expected), default to MIN if absent
    String lastUpdatedStr = checkpointService.getLastUpdated(projectKey);
    LocalDateTime lastUpdated = parseIsoLocalDateTimeOrMin(lastUpdatedStr);

    return issueRepository
        .findByProjectKeyAndUpdatedAfter(projectKey, lastUpdated)
        // Collect results into a list so we can determine the max timestamp afterward
        .collectList()
        .flatMap(
            list -> {
              if (list.isEmpty()) return Mono.just("Nothing new");

              return Mono.fromCallable(
                      () -> {
                        // Append new rows to the existing Excel workbook
                        writer.append(projectKey, list);
                        // Determine and store the new maximum updated timestamp
                        LocalDateTime newMax =
                            list.stream()
                                .map(Issue::getUpdated)
                                .filter(java.util.Objects::nonNull)
                                .max(Comparator.naturalOrder())
                                .orElse(lastUpdated);
                        // Persist checkpoint in ISO instant format with 'Z' to keep a canonical UTC
                        // value
                        checkpointService.writeLastUpdated(
                            projectKey, formatAsIsoInstantUtc(newMax));
                        return "Sync completed successfully";
                      })
                  // File I/O is blocking; execute it on a dedicated scheduler
                  .subscribeOn(Schedulers.boundedElastic());
            })
        .doOnError(e -> log.error("Error during sync", e));
  }

  private LocalDateTime parseIsoLocalDateTimeOrMin(String value) {
    if (value == null || value.isBlank()) return LocalDateTime.MIN;
    try {
      // First try plain LocalDateTime (no zone/offset)
      return LocalDateTime.parse(value);
    } catch (Exception ignored) {
      try {
        // Handle values with 'Z' or offset by normalizing to UTC LocalDateTime
        // Examples: 2025-01-02T00:00:00Z, 2025-01-02T00:00:00+01:00
        java.time.OffsetDateTime odt = java.time.OffsetDateTime.parse(value);
        return odt.withOffsetSameInstant(java.time.ZoneOffset.UTC).toLocalDateTime();
      } catch (Exception ignored2) {
        try {
          java.time.Instant instant = java.time.Instant.parse(value);
          return java.time.LocalDateTime.ofInstant(instant, java.time.ZoneOffset.UTC);
        } catch (Exception ex) {
          // Fallback to MIN on parse errors
          log.warn("Failed to parse lastUpdated '{}' , defaulting to MIN", value);
          return LocalDateTime.MIN;
        }
      }
    }
  }

  private String formatAsIsoInstantUtc(LocalDateTime value) {
    // Treat the LocalDateTime as UTC and format with 'Z'
    return java.time.OffsetDateTime.of(value, java.time.ZoneOffset.UTC).toInstant().toString();
  }
}

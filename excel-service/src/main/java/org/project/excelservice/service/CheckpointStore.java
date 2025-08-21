package org.project.excelservice.service;

import java.io.IOException;
import java.nio.file.*;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Persists the last processed issue id for every project so that the next sync starts where the
 * previous one stopped. The implementation is intentionally simple: one text file per project
 * containing the id as a long.
 *
 * <p>Because file access is blocking, callers should execute operations on a bounded-elastic
 * scheduler when used inside a reactive pipeline.
 */
@Component
@Slf4j
public class CheckpointStore {

  private final Path dir;

  public CheckpointStore(@Value("${excel.checkpoint.dir:reports/checkpoints}") String directory) {
    this.dir = Paths.get(directory);
  }

  /**
   * Read the last processed id for the given project.
   *
   * @return empty if the project was never processed.
   */
  public Optional<Long> read(String projectKey) {
    try {
      Path file = checkpointFile(projectKey);
      if (!Files.exists(file)) return Optional.empty();
      String content = Files.readString(file).trim();
      return content.isBlank() ? Optional.empty() : Optional.of(Long.parseLong(content));
    } catch (IOException | NumberFormatException e) {
      log.error("Failed to read checkpoint for {}", projectKey, e);
      return Optional.empty();
    }
  }

  /** Persist the provided id as the last processed checkpoint for the project. */
  public void write(String projectKey, long id) {
    try {
      Files.createDirectories(dir);
      Path file = checkpointFile(projectKey);
      Files.writeString(
          file, Long.toString(id), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
      log.debug("Checkpoint {} saved for project {}", id, projectKey);
    } catch (IOException e) {
      throw new RuntimeException("Failed to write checkpoint for " + projectKey, e);
    }
  }

  // ---------------------------------------------------------------------
  // Timestamp-based checkpoints (ISO-8601 strings)
  // ---------------------------------------------------------------------

  /** Read the last processed ISO-8601 timestamp for the given project. */
  public Optional<String> readTimestamp(String projectKey) {
    return readStringFile(timestampFile(projectKey));
  }

  /** Persist the provided ISO-8601 timestamp as checkpoint for next run. */
  public void writeTimestamp(String projectKey, String isoTimestamp) {
    writeStringFile(timestampFile(projectKey), isoTimestamp);
  }

  /** Convenience wrapper returning a default value when absent */
  public String getLastUpdated(String projectKey) {
    return readTimestamp(projectKey).orElse("1970-01-01T00:00:00Z");
  }

  public void writeLastUpdated(String projectKey, String isoTimestamp) {
    writeTimestamp(projectKey, isoTimestamp);
  }

  // ---------------------------------------------------------------------
  // Private helpers
  // ---------------------------------------------------------------------

  private Path checkpointFile(String projectKey) {
    return dir.resolve(projectKey + ".checkpoint");
  }

  private Path timestampFile(String projectKey) {
    return dir.resolve(projectKey + ".ts.checkpoint");
  }

  private Optional<String> readStringFile(Path file) {
    try {
      if (!Files.exists(file)) return Optional.empty();
      String content = Files.readString(file).trim();
      return content.isBlank() ? Optional.empty() : Optional.of(content);
    } catch (IOException e) {
      log.error("Failed to read checkpoint file {}", file, e);
      return Optional.empty();
    }
  }

  private void writeStringFile(Path file, String content) {
    try {
      Files.createDirectories(dir);
      Files.writeString(
          file, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    } catch (IOException e) {
      throw new RuntimeException("Failed to write checkpoint file " + file, e);
    }
  }
}

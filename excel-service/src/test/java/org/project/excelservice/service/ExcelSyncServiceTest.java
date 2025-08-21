package org.project.excelservice.service;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.project.excelservice.entity.Issue;
import org.project.excelservice.repository.IssueRepository;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

class ExcelSyncServiceTest {

  ExcelFileWriter writer;
  CheckpointStore checkpoint;
  IssueRepository repository;
  ExcelSyncService service;

  @BeforeEach
  void setUp() {
    writer = Mockito.mock(ExcelFileWriter.class);
    checkpoint = Mockito.mock(CheckpointStore.class);
    repository = Mockito.mock(IssueRepository.class);
    service = new ExcelSyncService(writer, checkpoint, repository);
  }

  @Test
  @DisplayName("sync() returns 'Nothing new' when no issues found")
  void sync_noIssues() {
    Mockito.when(checkpoint.getLastUpdated("PROJ")).thenReturn("1970-01-01T00:00:00Z");
    Mockito.when(
            repository.findByProjectKeyAndUpdatedAfter("PROJ", LocalDateTime.of(1970, 1, 1, 0, 0)))
        .thenReturn(Flux.empty());

    StepVerifier.create(service.sync("PROJ")).expectNext("Nothing new").verifyComplete();
  }

  @Test
  @DisplayName("sync() appends issues, updates checkpoint and returns success message")
  void sync_happyPath() {
    Issue i1 =
        Issue.builder()
            .issueKey("PROJ-1")
            .updated(LocalDateTime.parse("2025-01-01T00:00:01"))
            .build();
    Issue i2 =
        Issue.builder()
            .issueKey("PROJ-2")
            .updated(LocalDateTime.parse("2025-01-02T00:00:00"))
            .build();
    List<Issue> list = List.of(i1, i2);

    Mockito.when(checkpoint.getLastUpdated("PROJ")).thenReturn("1970-01-01T00:00:00Z");
    Mockito.when(
            repository.findByProjectKeyAndUpdatedAfter("PROJ", LocalDateTime.of(1970, 1, 1, 0, 0)))
        .thenReturn(Flux.fromIterable(list));

    StepVerifier.create(service.sync("PROJ"))
        .expectNext("Sync completed successfully")
        .verifyComplete();

    // verify writer append called
    Mockito.verify(writer).append("PROJ", list);

    // verify checkpoint updated with max updated timestamp
    ArgumentCaptor<String> cap = ArgumentCaptor.forClass(String.class);
    Mockito.verify(checkpoint).writeLastUpdated(Mockito.eq("PROJ"), cap.capture());
    org.assertj.core.api.Assertions.assertThat(cap.getValue()).isEqualTo("2025-01-02T00:00:00Z");
  }
}

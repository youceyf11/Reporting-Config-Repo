package org.project.jirafetchservice.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.project.jirafetchservice.dto.IssueSimpleDto;
import org.project.jirafetchservice.jiraApi.JiraIssueApiResponse;
import org.project.jirafetchservice.mapper.JiraMapper;
import org.project.jirafetchservice.service.JiraIssueService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/jira")
@Validated
public class JiraIssueController {

  private final JiraIssueService jiraIssueService;
  private final JiraMapper jiraMapper;

  public JiraIssueController(JiraIssueService jiraIssueService, JiraMapper jiraMapper) {
    this.jiraIssueService = jiraIssueService;
    this.jiraMapper = jiraMapper;
  }

  // ================== ENDPOIntegerS ISSUES ==================

  @GetMapping("/issues/{issueKey}")
  public Mono<ResponseEntity<JiraIssueApiResponse>> getIssue(
      @PathVariable
          @NotBlank
          @Pattern(regexp = "^[A-Z]+-\\d+$", message = "Format d'issue key invalide")
          String issueKey) {
    return jiraIssueService
        .getIssue(issueKey)
        .map(issue -> ResponseEntity.ok(issue))
        .onErrorReturn(ResponseEntity.notFound().build());
  }

  @GetMapping("/issues/{issueKey}/project")
  public Mono<String> getProjectKeyFromIssue(@PathVariable String issueKey) {
    return jiraIssueService.getProjectKeyFromIssue(issueKey);
  }

  // ================== ENDPOIntegerS PROJETS ==================

  @GetMapping("/projects/local")
  public Flux<String> getAllLocalProjectKeys() {
    return jiraIssueService.getAllLocalProjectKeys();
  }

  @GetMapping("/projects/{projectKey}/issues")
  public Flux<IssueSimpleDto> getProjectIssues(
      @PathVariable @NotBlank String projectKey,
      @RequestParam(defaultValue = "50") @Positive @Max(100) Integer limit) {
    return jiraIssueService
        .getProjectIssues(projectKey)
        .map(jiraMapper::toSimpleDtoFromApi)
        .take(limit);
  }

  // ================== ENDPOIntegerS RECHERCHE ==================

  @GetMapping("/search")
  public Flux<JiraIssueApiResponse> searchIssues(
      @RequestParam @NotBlank String jql,
      @RequestParam(defaultValue = "50") @Positive @Max(100) Integer limit) {
    return jiraIssueService.searchIssues(jql).take(limit);
  }

  @GetMapping("/assignees/{email}/issues")
  public Flux<JiraIssueApiResponse> getIssuesAssignedTo(@PathVariable @NotBlank String email) {
    return jiraIssueService.getIssuesAssignedTo(email);
  }

  // ================== ENDPOIntegerS SYNCHRONISATION ==================

  @PostMapping("/issues/{issueKey}/sync")
  public Mono<ResponseEntity<IssueSimpleDto>> syncIssue(@PathVariable @NotBlank String issueKey) {
    return jiraIssueService
        .synchroniserIssueAvecJira(issueKey)
        .map(issue -> ResponseEntity.ok(issue))
        .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
  }

  @PostMapping("/projects/{projectKey}/sync")
  public Flux<IssueSimpleDto> syncProject(
      @PathVariable @NotBlank String projectKey,
      @RequestParam(defaultValue = "50") @Positive @Max(100) Integer batchSize) {
    return jiraIssueService.synchroniserProjetAvecJira(projectKey, batchSize);
  }

  @PostMapping("/search/sync")
  public Flux<IssueSimpleDto> syncSearch(@RequestParam @NotBlank String jql) {
    return jiraIssueService.synchroniserSearchAvecJira(jql);
  }

  // ================== ENDPOIntegerS BASE LOCALE ==================

  @GetMapping("/local/issues/{issueKey}")
  public Mono<ResponseEntity<IssueSimpleDto>> getLocalIssue(
      @PathVariable @NotBlank String issueKey) {
    return jiraIssueService
        .recupererIssueLocale(issueKey)
        .map(issue -> ResponseEntity.ok(issue))
        .onErrorReturn(ResponseEntity.notFound().build());
  }

  // ================== ENDPOIntegerS UTILITAIRES ==================

  @GetMapping("/health")
  public Mono<ResponseEntity<String>> health() {
    return Mono.just(ResponseEntity.ok("Service Jira opérationnel"));
  }
}

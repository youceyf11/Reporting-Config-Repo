package org.project.jirafetchservice.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.project.jirafetchservice.dto.IssueSimpleDto;
import org.project.jirafetchservice.jirapi.JiraIssueApiResponse;
import org.project.jirafetchservice.mapper.JiraMapper;
import org.project.jirafetchservice.service.JiraIssueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/jira")
@Validated
public class JiraIssueController {

  private static final Logger log = LoggerFactory.getLogger(JiraIssueController.class);

  private final JiraIssueService jiraIssueService;
  private final JiraMapper jiraMapper;

  public JiraIssueController(JiraIssueService jiraIssueService, JiraMapper jiraMapper) {
    this.jiraIssueService = jiraIssueService;
    this.jiraMapper = jiraMapper;
  }

  // ================== ENDPOINTS ISSUES ==================

  @GetMapping("/issues/{issueKey}")
  public ResponseEntity<JiraIssueApiResponse> getIssue(
          @PathVariable
          @NotBlank
          @Pattern(regexp = "^[A-Z]+-\\d+$", message = "Format d'issue key invalide")
          String issueKey) {
    log.debug("Incoming GET /api/jira/issues/{}", issueKey);
    JiraIssueApiResponse issue = jiraIssueService.getIssue(issueKey);
    log.debug("Service returned issue: {}", issue == null ? "null" : "found");
    if (issue == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(issue);
  }

  @GetMapping("/issues/{issueKey}/project")
  public String getProjectKeyFromIssue(@PathVariable String issueKey) {
    log.debug("Incoming GET /api/jira/issues/{}/project", issueKey);
    return jiraIssueService.getProjectKeyFromIssue(issueKey);
  }

  // ================== ENDPOINTS PROJETS ==================

  @GetMapping("/projects/local")
  public List<String> getAllLocalProjectKeys() {
    return jiraIssueService.getAllLocalProjectKeys();
  }

  @GetMapping("/projects/{projectKey}/issues")
  public List<IssueSimpleDto> getProjectIssues(
          @PathVariable @NotBlank String projectKey,
          @RequestParam(defaultValue = "50") @Positive @Max(100) Integer limit) {
    List<JiraIssueApiResponse> issues = jiraIssueService.getProjectIssues(projectKey);
    return issues.stream()
            .map(jiraMapper::toSimpleDtoFromApi)
            .limit(limit)
            .collect(Collectors.toList());
  }

  // ================== ENDPOINTS RECHERCHE ==================

  @GetMapping("/search")
  public List<JiraIssueApiResponse> searchIssues(
          @RequestParam @NotBlank String jql,
          @RequestParam(defaultValue = "50") @Positive @Max(100) Integer limit) {
    List<JiraIssueApiResponse> issues = jiraIssueService.searchIssues(jql);
    return issues.stream().limit(limit).collect(Collectors.toList());
  }

  @GetMapping("/assignees/{email}/issues")
  public List<JiraIssueApiResponse> getIssuesAssignedTo(@PathVariable @NotBlank String email) {
    return jiraIssueService.getIssuesAssignedTo(email);
  }

  // ================== ENDPOINTS SYNCHRONISATION ==================

  @PostMapping("/issues/{issueKey}/sync")
  public ResponseEntity<IssueSimpleDto> syncIssue(@PathVariable @NotBlank String issueKey) {
    IssueSimpleDto issue = jiraIssueService.synchronizeIssueWithJira(issueKey);
    if (issue == null) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
    return ResponseEntity.ok(issue);
  }

  @PostMapping("/projects/{projectKey}/sync")
  public List<IssueSimpleDto> syncProject(
          @PathVariable @NotBlank String projectKey,
          @RequestParam(defaultValue = "50") @Positive @Max(100) Integer batchSize) {
    return jiraIssueService.synchronizeProjectWithJira(projectKey, batchSize);
  }

  @PostMapping("/search/sync")
  public List<IssueSimpleDto> syncSearch(@RequestParam @NotBlank String jql) {
    return jiraIssueService.synchronizeSearchWithJira(jql);
  }

  // ================== ENDPOINTS BASE LOCALE ==================

  @GetMapping("/local/issues/{issueKey}")
  public ResponseEntity<IssueSimpleDto> getLocalIssue(
          @PathVariable @NotBlank String issueKey) {
    log.debug("Incoming GET /api/jira/local/issues/{}", issueKey);
    IssueSimpleDto issue = jiraIssueService.getLocalIssue(issueKey);
    if (issue == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(issue);
  }

  // ================== ENDPOINTS UTILITAIRES ==================

  @GetMapping("/health")
  public ResponseEntity<String> health() {
    return ResponseEntity.ok("Service Jira opérationnel");
  }
}

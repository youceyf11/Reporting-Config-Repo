package org.project.jirafetchservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.project.jirafetchservice.dto.IssueSimpleDto;
import org.project.jirafetchservice.entity.JiraIssueDbEntity;
import org.project.jirafetchservice.jirapi.JiraIssueApiResponse;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@Mapper(componentModel = "spring")
public interface JiraMapper {

  // 1. API -> DB Entity
  @Mapping(source = "key", target = "issueKey")
  @Mapping(source = "id", target = "jiraId")
  @Mapping(source = "key", target = "projectKey", qualifiedByName = "extractProjectKey")
  @Mapping(source = "self", target = "selfUrl")
  @Mapping(source = "fields.summary", target = "summary")
  @Mapping(source = "fields.issuetype.name", target = "issueType")
  @Mapping(source = "fields.status.name", target = "status")
  @Mapping(source = "fields.priority.name", target = "priority")
  @Mapping(source = "fields.resolution.name", target = "resolution")
  @Mapping(source = "fields.assignee.displayName", target = "assignee")
  @Mapping(source = "fields.assignee.emailAddress", target = "assigneeEmail")
  @Mapping(source = "fields.reporter.displayName", target = "reporter")
  @Mapping(source = "fields.reporter.emailAddress", target = "reporterEmail")
  @Mapping(source = "fields.created", target = "created", qualifiedByName = "parseJiraDate")
  @Mapping(source = "fields.updated", target = "updated", qualifiedByName = "parseJiraDate")
  @Mapping(source = "fields.resolved", target = "resolved", qualifiedByName = "parseJiraDate")

  // Agile Metrics
  @Mapping(source = "fields.timeSpentSeconds", target = "timeSpentSeconds")
  @Mapping(source = "fields.originalEstimateSeconds", target = "originalEstimateSeconds")
  @Mapping(source = "fields.remainingEstimateSeconds", target = "remainingEstimateSeconds")
  @Mapping(source = "fields.storyPoints", target = "storyPoints")
  JiraIssueDbEntity toDbEntityFromApi(JiraIssueApiResponse apiResponse);

  // 2. DB Entity -> Simple DTO
  IssueSimpleDto toSimpleDtoFromDb(JiraIssueDbEntity dbEntity);

  // 3. API -> Simple DTO (Read Only)
  @Mapping(source = "key", target = "issueKey")
  @Mapping(source = "key", target = "projectKey", qualifiedByName = "extractProjectKey")
  @Mapping(source = "fields.summary", target = "summary")
  @Mapping(source = "fields.issuetype.name", target = "issueType")
  @Mapping(source = "fields.status.name", target = "status")
  @Mapping(source = "fields.priority.name", target = "priority")
  @Mapping(source = "fields.resolution.name", target = "resolution")
  @Mapping(source = "fields.assignee.displayName", target = "assignee")
  @Mapping(source = "fields.reporter.displayName", target = "reporter")
  @Mapping(source = "fields.created", target = "created", qualifiedByName = "parseJiraDate")
  @Mapping(source = "fields.updated", target = "updated", qualifiedByName = "parseJiraDate")
  @Mapping(source = "fields.resolved", target = "resolved", qualifiedByName = "parseJiraDate")
  @Mapping(source = "fields.timeSpentSeconds", target = "timeSpentSeconds")
  @Mapping(source = "fields.originalEstimateSeconds", target = "originalEstimateSeconds")
  @Mapping(source = "fields.storyPoints", target = "storyPoints")
  IssueSimpleDto toSimpleDtoFromApi(JiraIssueApiResponse apiResponse);

  @Named("parseJiraDate")
  default LocalDateTime parseJiraDate(String dateString) {
    if (dateString == null || dateString.isEmpty()) return null;
    try {
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
      return OffsetDateTime.parse(dateString, formatter).toLocalDateTime();
    } catch (Exception e) {
      try { return OffsetDateTime.parse(dateString).toLocalDateTime(); } catch(Exception ex) { return null; }
    }
  }

  @Named("extractProjectKey")
  default String extractProjectKey(String issueKey) {
    if (issueKey == null || !issueKey.contains("-")) return null;
    return issueKey.split("-")[0];
  }
}
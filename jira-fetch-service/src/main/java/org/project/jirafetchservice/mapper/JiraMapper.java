package org.project.jirafetchservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.project.jirafetchservice.dto.IssueSimpleDto;
import org.project.jirafetchservice.dto.JiraIssueDto;
import org.project.jirafetchservice.entity.JiraIssueDbEntity;
import org.project.jirafetchservice.jiraApi.JiraIssueApiResponse;
import java.time.format.DateTimeFormatter;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

@Mapper(componentModel = "spring")
public interface JiraMapper {

    // API → DTO simple
    @Mapping(source = "key", target = "issueKey")
    @Mapping(source = "key", target = "projectKey", qualifiedByName = "extractProjectKey")
    @Mapping(source = "fields.summary", target = "summary")
    @Mapping(source = "fields.issuetype.name", target = "issueType")
    @Mapping(source = "fields.status.name", target = "status")
    @Mapping(source = "fields.priority.name", target = "priority")
    @Mapping(source = "fields.resolution.name", target = "resolution")
    @Mapping(source = "fields.assignee.displayName", target = "assignee")
    @Mapping(source = "fields.reporter.displayName", target = "reporter")
    @Mapping(source = "fields.organization", target = "organization")
    @Mapping(source = "fields.created", target = "created", qualifiedByName = "parseJiraDate")
    @Mapping(source = "fields.updated", target = "updated", qualifiedByName = "parseJiraDate")
    @Mapping(source = "fields.resolved", target = "resolved", qualifiedByName = "parseJiraDate")
    @Mapping(source = "fields.timeSpentSeconds", target = "timeSpentSeconds")
    @Mapping(source = "fields.classification", target = "classification")
    @Mapping(source = "fields.entity", target = "entity")
    @Mapping(source = "fields.issueQuality", target = "issueQuality")
    @Mapping(source = "fields.medium", target = "medium")
    @Mapping(source = "fields.ttsDays", target = "ttsDays")
    @Mapping(source = "fields.site", target = "site")
    @Mapping(source = "fields.month", target = "month")
    @Mapping(source = "fields.quotaPerProject", target = "quotaPerProject")
    IssueSimpleDto toSimpleDtoFromApi(JiraIssueApiResponse apiResponse);

    @Named("parseJiraDate")
    default LocalDateTime parseJiraDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return null;
        }
        try {
            // single 'X' zone-offset token is supported on Java 8+; double 'XX' fails on older JDKs used in CI
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
            return OffsetDateTime.parse(dateString, formatter).toLocalDateTime();
        } catch (DateTimeParseException e) {
            System.err.println("Error parsing date: " + dateString + " - " + e.getMessage());
            return null;
        }
    }

    @Named("extractProjectKey")
    default String extractProjectKey(String issueKey) {
        if (issueKey == null || !issueKey.contains("-")) {
            return null;
        }
        return issueKey.split("-")[0];
    }

    @Named("formatDateForDb")
    default String formatDateForDb(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    // API → DTO complet
    @Mapping(source = "fields.summary", target = "summary")
    @Mapping(source = "key", target = "issueKey")
    @Mapping(source = "key", target = "projectKey", qualifiedByName = "extractProjectKey")
    @Mapping(source = "fields.issuetype.name", target = "issueType")
    @Mapping(source = "fields.status.name", target = "status")
    @Mapping(source = "fields.priority.name", target = "priority")
    @Mapping(source = "fields.resolution.name", target = "resolution")
    @Mapping(source = "fields.assignee.displayName", target = "assignee")
    @Mapping(source = "fields.assignee.emailAddress", target = "assigneeEmail")
    @Mapping(source = "fields.reporter.displayName", target = "reporter")
    @Mapping(source = "fields.reporter.emailAddress", target = "reporterEmail")
    @Mapping(source = "fields.organization", target = "organization")
    @Mapping(source = "fields.created", target = "created")
    @Mapping(source = "fields.updated", target = "updated")
    @Mapping(source = "fields.resolved", target = "resolved")
    @Mapping(source = "fields.timeSpentSeconds", target = "timeSpentSeconds")
    @Mapping(source = "fields.classification", target = "classification")
    @Mapping(source = "fields.entity", target = "entity")
    @Mapping(source = "fields.issueQuality", target = "issueQuality")
    @Mapping(source = "fields.medium", target = "medium")
    @Mapping(source = "fields.ttsDays", target = "ttsDays")
    @Mapping(source = "fields.site", target = "site")
    @Mapping(source = "fields.month", target = "month")
    @Mapping(source = "fields.quotaPerProject", target = "quotaPerProject")
    @Mapping(source = "self", target = "self")
    JiraIssueDto toJiraIssueDto(JiraIssueApiResponse apiResponse);

    // API → Entité DB
    @Mapping(source = "key", target = "id")
    @Mapping(source = "key", target = "issueKey")
    @Mapping(source = "key", target = "projectKey", qualifiedByName = "extractProjectKey")
    @Mapping(source = "fields.summary", target = "summary")
    @Mapping(source = "fields.issuetype.name", target = "issueType")
    @Mapping(source = "fields.status.name", target = "status")
    @Mapping(source = "fields.priority.name", target = "priority")
    @Mapping(source = "fields.resolution.name", target = "resolution")
    @Mapping(source = "fields.assignee.displayName", target = "assignee")
    @Mapping(source = "fields.assignee.emailAddress", target = "assigneeEmail")
    @Mapping(source = "fields.reporter.displayName", target = "reporter")
    @Mapping(source = "fields.reporter.emailAddress", target = "reporterEmail")
    @Mapping(source = "fields.organization", target = "organization")
    @Mapping(source = "fields.created", target = "created", qualifiedByName = "parseJiraDate")
    @Mapping(source = "fields.updated", target = "updated", qualifiedByName = "parseJiraDate")
    @Mapping(source = "fields.resolved", target = "resolved", qualifiedByName = "parseJiraDate")
    @Mapping(source = "fields.timeSpentSeconds", target = "timeSpentSeconds")
    @Mapping(source = "fields.classification", target = "classification")
    @Mapping(source = "fields.entity", target = "entity")
    @Mapping(source = "fields.issueQuality", target = "issueQuality")
    @Mapping(source = "fields.medium", target = "medium")
    @Mapping(source = "fields.ttsDays", target = "ttsDays")
    @Mapping(source = "fields.site", target = "site")
    @Mapping(source = "fields.month", target = "issueMonth")
    @Mapping(source = "fields.quotaPerProject", target = "quotaPerProject")
    @Mapping(target = "newEntity", ignore = true) 
    @Mapping(source = "self", target = "selfUrl")
    JiraIssueDbEntity toDbEntityFromApi(JiraIssueApiResponse apiResponse);

    // Entité DB → DTO simple
    @Mapping(source = "issueMonth", target = "month")
    IssueSimpleDto toSimpleDtoFromDb(JiraIssueDbEntity dbEntity);

    // DTO simple → Entité DB
    @Mapping(source = "issueKey", target = "id")
    @Mapping(source = "issueKey", target = "issueKey")
    @Mapping(source = "issueKey", target = "projectKey", qualifiedByName = "extractProjectKey")
    @Mapping(target = "newEntity", ignore = true) // conserve la valeur par défaut true
    @Mapping(source = "created", target = "created")
    @Mapping(source = "updated", target = "updated")
    @Mapping(source = "resolved", target = "resolved")
    @Mapping(source = "month", target = "issueMonth")
    @Mapping(target = "assigneeEmail", ignore = true)
    @Mapping(target = "reporterEmail", ignore = true)
    @Mapping(target = "selfUrl", ignore = true)
    JiraIssueDbEntity toDbEntityFromSimpleDto(IssueSimpleDto dto);

    // Méthodes pour les listes
    List<IssueSimpleDto> toSimpleDtoListFromApi(List<JiraIssueApiResponse> apiList);
    List<JiraIssueDto> toJiraIssueDtoList(List<JiraIssueApiResponse> apiList);
    List<JiraIssueDbEntity> toDbEntityListFromApi(List<JiraIssueApiResponse> apiList);
    List<IssueSimpleDto> toSimpleDtoListFromDb(List<JiraIssueDbEntity> dbList);
}
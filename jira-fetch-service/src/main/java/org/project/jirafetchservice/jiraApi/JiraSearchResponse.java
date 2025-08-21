package org.project.jirafetchservice.jiraApi;

import java.util.List;

public class JiraSearchResponse {
  private List<JiraIssueApiResponse> issues;
  private Integer total;

  public List<JiraIssueApiResponse> getIssues() {
    return issues;
  }

  public void setIssues(List<JiraIssueApiResponse> issues) {
    this.issues = issues;
  }

  public Integer getTotal() {
    return total;
  }

  public void setTotal(Integer total) {
    this.total = total;
  }
}

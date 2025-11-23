package org.project.jirafetchservice.jirapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class JiraSearchResponse {


  private List<JiraIssueApiResponse> issues;
  private Integer total;

}

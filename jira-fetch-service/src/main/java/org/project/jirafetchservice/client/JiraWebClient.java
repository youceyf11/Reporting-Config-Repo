package org.project.jirafetchservice.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.project.jirafetchservice.jirapi.JiraIssueApiResponse;
import org.project.jirafetchservice.jirapi.JiraSearchResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class JiraWebClient {

    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String authHeaderValue;
    private final HttpClient httpClient;

    public JiraWebClient(ObjectMapper objectMapper,
            @Value("${jira.base-url}") String baseUrl,
            @Value("${jira.username}") String username,
            @Value("${jira.api-token}") String apiToken) {
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;

        String cleanToken = apiToken.replace("\"", "").trim();
        String cleanUser = username.trim();
        String auth = cleanUser + ":" + cleanToken;
        this.authHeaderValue = "Basic " + Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        this.httpClient = HttpClient.newHttpClient();
    }

    public JiraIssueApiResponse getIssue(String issueKey) {
        try {
            // Keep v3 for single issue if it works, or switch to v2 if this fails too
            String uri = baseUrl + "/rest/api/3/issue/" + issueKey;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(uri))
                    .header("Authorization", authHeaderValue)
                    .header("Accept", "application/json")
                    .GET().build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200)
                return null;
            return objectMapper.readValue(response.body(), JiraIssueApiResponse.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    public JiraSearchResponse searchIssues(String jql) {
        try {
            // Correct endpoint and param name
            String uri = baseUrl + "/rest/api/3/search/jql?jql=" + java.net.URLEncoder.encode(jql, java.nio.charset.StandardCharsets.UTF_8) +
                    "&startAt=0&maxResults=50&fields=*all";

            System.out.println("⚡️ USING NEW JQL SEARCH: " + uri);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(uri))
                    .header("Authorization", authHeaderValue)
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("❌ JIRA JQL ERROR: " + response.statusCode());
                System.err.println("❌ BODY: " + response.body());
                throw new RuntimeException("Jira Search Failed: " + response.statusCode());
            }

            return objectMapper.readValue(response.body(), JiraSearchResponse.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to search Jira issues", e);
        }
    }
    public List<JiraIssueApiResponse> searchProjectIssues(String projectKey) {
        // Enclose project key in quotes
        String jql = "project = \"" + projectKey + "\"";
        JiraSearchResponse response = searchIssues(jql);
        return response != null ? response.getIssues() : List.of();
    }
}
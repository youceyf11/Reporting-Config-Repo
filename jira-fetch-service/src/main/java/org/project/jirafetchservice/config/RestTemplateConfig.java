package org.project.jirafetchservice.config;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import lombok.Getter;
import lombok.Setter;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

@Configuration
@Getter
@Setter
public class RestTemplateConfig {

    @Value("${jira.base-url}")
    private String jiraBaseUrl;

    @Value("${jira.username}")
    private String jiraUsername;

    @Value("${jira.api-token}")
    private String jiraApiToken;

    @Bean
    public RestTemplate restTemplate() {
        String credentials =
                Base64.getEncoder()
                        .encodeToString((jiraUsername + ":" + jiraApiToken).getBytes(StandardCharsets.UTF_8));

        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);

        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.getInterceptors().add((request, body, execution) -> {
            request.getHeaders().add(HttpHeaders.AUTHORIZATION, "Basic " + credentials);
            request.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            return execution.execute(request, body);
        });

        return restTemplate;
    }

}
package org.project.issueevents;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class IssueEventsApplication {

  public static void main(String[] args) {
    SpringApplication.run(IssueEventsApplication.class, args);
  }
}

package org.project.chartservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class ChartServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(ChartServiceApplication.class, args);
  }
}

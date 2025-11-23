package org.project.reportingservice.repository;

import org.project.reportingservice.entity.EmployeePerformanceMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeMetricRepository extends JpaRepository<EmployeePerformanceMetric, Long> {

    Optional<EmployeePerformanceMetric> findByEmployeeEmailAndMetricPeriod(String email, String period);
    List<EmployeePerformanceMetric> findByMetricPeriod(String period);
}
-- Reporting Service Database Schema
-- Tables for storing reports, metrics, and analytics data

-- Reports table
CREATE TABLE IF NOT EXISTS reports (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    report_type VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    status VARCHAR(50) DEFAULT 'ACTIVE'
);

-- Report executions table
CREATE TABLE IF NOT EXISTS report_executions (
    id BIGSERIAL PRIMARY KEY,
    report_id BIGINT NOT NULL REFERENCES reports(id),
    execution_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) DEFAULT 'PENDING',
    result_data JSONB,
    error_message TEXT,
    execution_time_ms BIGINT
);

-- Metrics table for storing aggregated data
CREATE TABLE IF NOT EXISTS metrics (
    id BIGSERIAL PRIMARY KEY,
    metric_name VARCHAR(255) NOT NULL,
    metric_value DECIMAL(15,2),
    metric_date DATE NOT NULL,
    tags JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_reports_type ON reports(report_type);
CREATE INDEX IF NOT EXISTS idx_reports_status ON reports(status);
CREATE INDEX IF NOT EXISTS idx_report_executions_report_id ON report_executions(report_id);
CREATE INDEX IF NOT EXISTS idx_report_executions_date ON report_executions(execution_date);
CREATE INDEX IF NOT EXISTS idx_metrics_name_date ON metrics(metric_name, metric_date);
CREATE INDEX IF NOT EXISTS idx_metrics_date ON metrics(metric_date);

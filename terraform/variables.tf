variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "eu-central-1"
}

variable "project_name" {
  description = "Project name used for resource naming"
  type        = string
  default     = "reporting-logistics"
}

variable "vpc_cidr" {
  description = "CIDR block for VPC"
  type        = string
  default     = "10.0.0.0/16"
}

variable "service_names" {
  description = "List of microservice names"
  type        = list(string)
  default = [
    "gateway-service",
    "jira-fetch-service",
    "reporting-service",
    "chart-service",
    "email-service",
    "excel-service"
  ]
}

# Database variables
variable "db_name" {
  description = "PostgreSQL database name"
  type        = string
  default     = "reporting_db"
}

variable "db_username" {
  description = "PostgreSQL username"
  type        = string
  default     = "postgres"
}

variable "db_password" {
  description = "PostgreSQL password"
  type        = string
  sensitive   = true
}

# ECS Task Configuration (Free Tier optimized)
variable "task_cpu" {
  description = "CPU units for ECS tasks (256 = 0.25 vCPU)"
  type        = number
  default     = 256 # Minimum for Fargate, Free Tier friendly
}

variable "task_memory" {
  description = "Memory for ECS tasks in MB"
  type        = number
  default     = 512 # Minimum for Fargate, Free Tier friendly
}

variable "desired_count" {
  description = "Desired number of tasks per service"
  type        = number
  default     = 1 # Single instance to minimize costs
}

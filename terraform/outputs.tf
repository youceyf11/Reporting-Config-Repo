# Outputs for important resource information
output "vpc_id" {
  description = "ID of the VPC"
  value       = aws_vpc.main.id
}

output "public_subnet_ids" {
  description = "IDs of the public subnets"
  value       = aws_subnet.public[*].id
}

output "ecs_cluster_name" {
  description = "Name of the ECS cluster"
  value       = aws_ecs_cluster.main.name
}

output "ecs_cluster_arn" {
  description = "ARN of the ECS cluster"
  value       = aws_ecs_cluster.main.arn
}

# ALB outputs commented out due to account restriction
# output "load_balancer_dns" {
#   description = "DNS name of the Application Load Balancer"
#   value       = aws_lb.main.dns_name
# }

# output "load_balancer_url" {
#   description = "URL to access the application"
#   value       = "http://${aws_lb.main.dns_name}"
# }

output "rds_endpoint" {
  description = "RDS PostgreSQL endpoint"
  value       = aws_db_instance.postgres.endpoint
  sensitive   = true
}

output "ecr_repository_urls" {
  description = "ECR repository URLs for each service"
  value = {
    for service_name, repo in aws_ecr_repository.services :
    service_name => repo.repository_url
  }
}

output "secret_arn" {
  description = "ARN of the Secrets Manager secret"
  value       = aws_secretsmanager_secret.app_config.arn
}

output "deployment_info" {
  description = "Important deployment information"
  value = {
    region           = var.aws_region
    cluster_name     = aws_ecs_cluster.main.name
    # load_balancer_url = "http://${aws_lb.main.dns_name}"  # Commented out due to ALB restriction
    database_endpoint = aws_db_instance.postgres.endpoint
    secret_name      = aws_secretsmanager_secret.app_config.name
  }
}

# Cost estimation output
output "estimated_monthly_cost" {
  description = "Estimated monthly cost breakdown (Free Tier)"
  value = {
    fargate_tasks    = "~$0 (Free Tier: 20GB-hours/month)"
    rds_postgres     = "~$0 (Free Tier: 750 hours db.t3.micro)"
    # application_lb   = "~$16.20 (750 hours + 15GB data processing)"  # Commented out - no ALB
    data_transfer    = "~$0 (Free Tier: 15GB out/month)"
    cloudwatch_logs  = "~$0 (Free Tier: 5GB ingestion)"
    secrets_manager  = "~$0.40 (1 secret)"
    ecr_storage      = "~$0 (Free Tier: 500MB)"
    total_estimated  = "~$0.40/month (after Free Tier, no ALB)"
  }
}

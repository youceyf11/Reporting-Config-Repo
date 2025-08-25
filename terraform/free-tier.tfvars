# Free Tier optimized configuration
aws_region   = "eu-central-1"
project_name = "reporting-logistics"

# Database configuration
db_name     = "reporting_db"
db_username = "postgres"
db_password = "Youssefouriniche11!"  

# ECS Configuration (Free Tier limits)
task_cpu      = 256  # 0.25 vCPU (minimum for Fargate)
task_memory   = 512  # 512 MB (minimum for Fargate)
desired_count = 1    # Single instance per service

# Network configuration
vpc_cidr = "10.0.0.0/16"

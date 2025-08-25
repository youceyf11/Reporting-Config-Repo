# ECS Task Definitions
resource "aws_ecs_task_definition" "services" {
  for_each = toset(var.service_names)

  family                   = each.key
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = var.task_cpu
  memory                   = var.task_memory
  execution_role_arn       = aws_iam_role.ecs_task_execution_role.arn

  container_definitions = jsonencode([
    {
      name  = each.key
      image = "${aws_ecr_repository.services[each.key].repository_url}:latest"

      portMappings = [
        {
          containerPort = local.service_ports[each.key]
          protocol      = "tcp"
        }
      ]

      environment = concat(
        [
          {
            name  = "SPRING_PROFILES_ACTIVE"
            value = "prod"
          }
        ],
        # Only add database config for services that need it (not gateway-service)
        each.key != "gateway-service" ? [
          {
            name  = "SPRING_DATASOURCE_URL"
            value = "jdbc:postgresql://${aws_db_instance.postgres.endpoint}/${aws_db_instance.postgres.db_name}"
          },
          {
            name  = "SPRING_R2DBC_URL"
            value = "r2dbc:postgresql://${aws_db_instance.postgres.endpoint}/${aws_db_instance.postgres.db_name}"
          }
        ] : [],
        each.key == "reporting-service" ? [
          {
            name  = "SPRING_SQL_INIT_MODE"
            value = "never"
          }
        ] : []
      )

      # Only add database secrets for services that need them (not gateway-service)
      secrets = each.key != "gateway-service" ? [
        {
          name      = "SPRING_R2DBC_USERNAME"
          valueFrom = "${aws_secretsmanager_secret.app_config.arn}:db.username::"
        },
        {
          name      = "SPRING_R2DBC_PASSWORD"
          valueFrom = "${aws_secretsmanager_secret.app_config.arn}:db.password::"
        },
        {
          name      = "SPRING_DATASOURCE_USERNAME"
          valueFrom = "${aws_secretsmanager_secret.app_config.arn}:db.username::"
        },
        {
          name      = "SPRING_DATASOURCE_PASSWORD"
          valueFrom = "${aws_secretsmanager_secret.app_config.arn}:db.password::"
        }
      ] : []

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.services[each.key].name
          "awslogs-region"        = var.aws_region
          "awslogs-stream-prefix" = "ecs"
        }
      }

      essential = true
    }
  ])

  tags = {
    Name = "${var.project_name}-${each.key}-task"
  }
}

# ECS Services
resource "aws_ecs_service" "services" {
  for_each = toset(var.service_names)

  name            = each.key
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.services[each.key].arn
  desired_count   = var.desired_count
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = aws_subnet.public[*].id
    security_groups  = [aws_security_group.ecs_tasks.id]
    assign_public_ip = true # Required for public subnets without NAT
  }

  # Only attach load balancer to gateway-service - COMMENTED OUT DUE TO ALB RESTRICTION
  # dynamic "load_balancer" {
  #   for_each = each.key == "gateway-service" ? [1] : []
  #   content {
  #     target_group_arn = aws_lb_target_group.gateway.arn
  #     container_name   = each.key
  #     container_port   = local.service_ports[each.key]
  #   }
  # }

  depends_on = [
    # aws_lb_listener.main,  # Commented out due to ALB restriction
    aws_iam_role_policy_attachment.ecs_task_execution_role_policy
  ]

  tags = {
    Name = "${var.project_name}-${each.key}-service"
  }
}

# Local values for service ports
locals {
  service_ports = {
    "gateway-service"    = 8086
    "jira-fetch-service" = 8081
    "reporting-service"  = 8082
    "chart-service"      = 8083
    "email-service"      = 8084
    "excel-service"      = 8085
  }
}

# Secrets Manager for application configuration
resource "aws_secretsmanager_secret" "app_config" {
  name        = "${var.project_name}/app-config"
  description = "Application configuration secrets"

  tags = {
    Name = "${var.project_name}-app-config"
  }
}

resource "aws_secretsmanager_secret_version" "app_config" {
  secret_id = aws_secretsmanager_secret.app_config.id
  secret_string = jsonencode({
    "db.username" = var.db_username
    "db.password" = var.db_password
    "db.url"      = "r2dbc:postgresql://${aws_db_instance.postgres.endpoint}/${aws_db_instance.postgres.db_name}"
  })

  depends_on = [aws_db_instance.postgres]
}

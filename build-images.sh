#!/bin/bash

# 1) Build services (skip tests for speed)
mvn -DskipTests clean package

# 2) Choose a unique tag (commit SHA) and login
export NEW_TAG=$(git rev-parse --short HEAD)
aws ecr get-login-password --region eu-central-1 | docker login --username AWS --password-stdin 054037124354.dkr.ecr.eu-central-1.amazonaws.com

# 3) Build and push only the services that still showed findings
services=(discovery-service gateway-service chart-service excel-service)

for svc in "${services[@]}"; do
  # Build Docker image with correct platform
  docker build --platform linux/amd64 -t "054037124354.dkr.ecr.eu-central-1.amazonaws.com/${svc}:${NEW_TAG}" "./${svc}"
  docker push "054037124354.dkr.ecr.eu-central-1.amazonaws.com/${svc}:${NEW_TAG}"

  # also tag latest if you want
  docker tag "054037124354.dkr.ecr.eu-central-1.amazonaws.com/${svc}:${NEW_TAG}" "054037124354.dkr.ecr.eu-central-1.amazonaws.com/${svc}:latest"
  docker push "054037124354.dkr.ecr.eu-central-1.amazonaws.com/${svc}:latest"
done

# 4) Rescan with Trivy (use the new flag; --vuln-type is deprecated)
for svc in "${services[@]}"; do
  trivy image --scanners vuln --pkg-types os,library --severity CRITICAL,HIGH \
    "054037124354.dkr.ecr.eu-central-1.amazonaws.com/${svc}:${NEW_TAG}"
done
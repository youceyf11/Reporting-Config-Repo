#!/usr/bin/env bash
set -e

# ---- configuration ----
export DOCKER_DEFAULT_PLATFORM=linux/arm64
BUILDER="paketobuildpacks/builder-jammy-base:latest"
ORG="youceyf11"
TAG="latest"
PLATFORM="linux/arm64"
services=(
  jira-fetch-service reporting-service chart-service
  email-service excel-service discovery-service gateway-service
)
# ------------------------

for svc in "${services[@]}"; do
  echo "🚀 Building $svc ..."
  ./mvnw -pl "$svc" spring-boot:build-image -DskipTests \
         -Dspring-boot.build-image.builder="$BUILDER" \
         -Dspring-boot.build-image.platform="$PLATFORM" \
         -Dspring-boot.build-image.imageName="ghcr.io/${ORG}/${svc}:${TAG}" \
         -Dspring-boot.build-image.env.BP_ARCH=arm64
  echo "✅ $svc completed"
done

echo "✅ All images built for $PLATFORM"

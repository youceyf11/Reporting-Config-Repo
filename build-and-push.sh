#!/bin/bash

# Automated Docker Build and Push Script for Reporting Logistics Microservices
# This script builds and pushes all 6 microservices to AWS ECR

set -e  # Exit on any error

# Configuration
AWS_REGION="eu-central-1"
AWS_ACCOUNT_ID="054037124354"
ECR_BASE_URL="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"

# Service definitions
SERVICES=("gateway-service" "jira-fetch-service" "reporting-service" "chart-service" "email-service" "excel-service")
PORTS=("8086" "8081" "8082" "8083" "8084" "8085")

echo "🚀 Starting automated Docker build and push process..."
echo "📍 Region: ${AWS_REGION}"
echo "🏢 Account: ${AWS_ACCOUNT_ID}"
echo "📦 Services: ${SERVICES[@]}"
echo ""

# Step 1: Login to ECR
echo "🔐 Logging into AWS ECR..."
aws ecr get-login-password --region ${AWS_REGION} | docker login --username AWS --password-stdin ${ECR_BASE_URL}

if [ $? -eq 0 ]; then
    echo "✅ ECR login successful"
else
    echo "❌ ECR login failed"
    exit 1
fi

echo ""

# Step 2: Build and push each service
for i in "${!SERVICES[@]}"; do
    service="${SERVICES[$i]}"
    port="${PORTS[$i]}"
    echo "🔨 Building ${service} (port: ${port})..."
    
    # Check if service directory exists
    if [ ! -d "./${service}" ]; then
        echo "⚠️  Directory ./${service} not found, skipping..."
        continue
    fi
    
    # Check if Dockerfile exists
    if [ ! -f "./${service}/Dockerfile" ]; then
        echo "⚠️  Dockerfile not found in ./${service}, skipping..."
        continue
    fi
    
    # Build the Docker image
    echo "   📦 Building Docker image..."
    docker build -t ${service}:latest ./${service}/
    
    if [ $? -eq 0 ]; then
        echo "   ✅ Build successful for ${service}"
    else
        echo "   ❌ Build failed for ${service}"
        continue
    fi
    
    # Tag for ECR
    echo "   🏷️  Tagging image for ECR..."
    docker tag ${service}:latest ${ECR_BASE_URL}/${service}:latest
    
    # Push to ECR
    echo "   ⬆️  Pushing to ECR..."
    docker push ${ECR_BASE_URL}/${service}:latest
    
    if [ $? -eq 0 ]; then
        echo "   ✅ Push successful for ${service}"
        echo "   📍 Image URL: ${ECR_BASE_URL}/${service}:latest"
    else
        echo "   ❌ Push failed for ${service}"
    fi
    
    echo ""
done

echo "🎉 Docker build and push process completed!"
echo ""
echo "📋 Summary:"
echo "   - All images are now available in ECR"
echo "   - ECS services can pull these images"
echo "   - Ready for Terraform deployment"
echo ""
echo "🔄 Next steps:"
echo "   1. Apply Terraform configuration: terraform apply -var-file='free-tier.tfvars'"
echo "   2. Check ECS services status in AWS Console"
echo "   3. Monitor CloudWatch logs for service health"

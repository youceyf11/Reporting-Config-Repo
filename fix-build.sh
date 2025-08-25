#!/bin/bash

echo "🧹 Cleaning corrupted Maven build artifacts..."

# Clean all target directories
find . -name "target" -type d -exec rm -rf {} + 2>/dev/null || true

# Clean Maven cache for this project
rm -rf ~/.m2/repository/org/project/ 2>/dev/null || true

echo "✅ Build artifacts cleaned"

echo "🔨 Rebuilding all services..."

# Build without tests first to ensure all classes are compiled
./mvnw clean compile -DskipTests

echo "📦 Building JARs..."

# Now build with packaging
./mvnw package -DskipTests

echo "✅ All services rebuilt successfully"

# Verify JARs exist
echo "📋 Verifying JAR files:"
for service in gateway-service jira-fetch-service reporting-service chart-service email-service excel-service; do
    jar_files=$(ls $service/target/*.jar 2>/dev/null | wc -l)
    if [ $jar_files -gt 0 ]; then
        jar_name=$(ls $service/target/*.jar 2>/dev/null | head -1)
        echo "✅ $service: $(basename $jar_name)"
    else
        echo "❌ $service: JAR missing"
    fi
done

echo "🚀 Ready for Docker build and push"

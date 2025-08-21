# 1) Build services (skip tests for speed)
mvn -DskipTests clean package

# 2) Choose a unique tag (commit SHA) and login
export NEW_TAG=$(git rev-parse --short HEAD)
echo "$YOUR_PAT" | docker login ghcr.io -u youceyf11 --password-stdin

# 3) Build and push only the services that still showed findings
services=(discovery-service gateway-service chart-service excel-service)

for svc in "${services[@]}"; do
  docker build -t "ghcr.io/youceyf11/${svc}:${NEW_TAG}" "./${svc}"
  docker push "ghcr.io/youceyf11/${svc}:${NEW_TAG}"

  # also tag latest if you want
  docker tag "ghcr.io/youceyf11/${svc}:${NEW_TAG}" "ghcr.io/youceyf11/${svc}:latest"
  docker push "ghcr.io/youceyf11/${svc}:latest"
done

# 4) Rescan with Trivy (use the new flag; --vuln-type is deprecated)
for svc in "${services[@]}"; do
  trivy image --scanners vuln --pkg-types os,library --severity CRITICAL,HIGH \
    "ghcr.io/youceyf11/${svc}:${NEW_TAG}"
done
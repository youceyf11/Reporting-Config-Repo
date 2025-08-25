# Terraform Infrastructure - Free Tier AWS Deployment

This Terraform configuration deploys your microservices application on AWS using **Free Tier compatible** services only.

## 🎯 What's Included (100% Free Tier)

### Infrastructure
- **VPC**: Custom VPC with public subnets (no NAT Gateway needed)
- **ECS Fargate**: Container orchestration (Free Tier: 20GB-hours/month)
- **RDS PostgreSQL**: Database (Free Tier: db.t3.micro, 750 hours/month)
- **Application Load Balancer**: Public access to gateway-service
- **ECR**: Container registry (Free Tier: 500MB storage)
- **Secrets Manager**: Secure credential storage
- **CloudWatch Logs**: Application logging (Free Tier: 5GB/month)

### Services Deployed
- `gateway-service` (port 8080) - behind ALB
- `jira-fetch-service` (port 8081)
- `reporting-service` (port 8082)
- `chart-service` (port 8083)
- `email-service` (port 8084)

## 💰 Cost Breakdown (Monthly)

| Service | Free Tier | After Free Tier |
|---------|-----------|-----------------|
| ECS Fargate | 20GB-hours FREE | ~$0 |
| RDS PostgreSQL | 750 hours FREE | ~$0 |
| Application LB | - | ~$16.20 |
| CloudWatch Logs | 5GB FREE | ~$0 |
| Secrets Manager | - | ~$0.40 |
| ECR Storage | 500MB FREE | ~$0 |
| **Total** | | **~$16.60/month** |

## 🚀 Quick Start

### Prerequisites
1. AWS Free Tier account
2. AWS CLI configured or GitHub Secrets set up
3. Terraform >= 1.0 installed (for local deployment)

### Option 1: GitHub Actions (Recommended)

1. **Set GitHub Secrets:**
   ```
   AWS_ACCESS_KEY_ID=your_access_key
   AWS_SECRET_ACCESS_KEY=your_secret_key
   DB_PASSWORD=YourSecurePassword123!
   ```

2. **Update password in tfvars:**
   ```bash
   # Edit terraform/free-tier.tfvars
   db_password = "YourActualSecurePassword!"
   ```

3. **Deploy via GitHub Actions:**
   - Go to Actions tab in GitHub
   - Run "Deploy Infrastructure with Terraform"
   - Choose: environment=`free-tier`, action=`apply`

### Option 2: Local Deployment

1. **Configure AWS credentials:**
   ```bash
   aws configure
   # or export AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY
   ```

2. **Deploy infrastructure:**
   ```bash
   cd terraform/
   terraform init
   terraform plan -var-file="free-tier.tfvars"
   terraform apply -var-file="free-tier.tfvars" -auto-approve
   ```

3. **Get outputs:**
   ```bash
   terraform output load_balancer_url
   terraform output ecr_repository_urls
   ```

## 📦 After Infrastructure Deployment

### 1. Push Docker Images to ECR

The infrastructure creates ECR repositories. Push your images:

```bash
# Get ECR login
aws ecr get-login-password --region eu-central-1 | docker login --username AWS --password-stdin <account-id>.dkr.ecr.eu-central-1.amazonaws.com

# Build and push each service
docker build -t gateway-service ./gateway-service
docker tag gateway-service:latest <account-id>.dkr.ecr.eu-central-1.amazonaws.com/gateway-service:latest
docker push <account-id>.dkr.ecr.eu-central-1.amazonaws.com/gateway-service:latest

# Repeat for: jira-fetch-service, reporting-service, chart-service, email-service
```

### 2. Monitor Deployment

- **ECS Console**: Check service status and task health
- **CloudWatch Logs**: Monitor application logs at `/ecs/{service-name}`
- **Load Balancer**: Access application at the ALB DNS name

## 🔧 Configuration Details

### Free Tier Optimizations
- **CPU/Memory**: 256 CPU units (0.25 vCPU), 512MB RAM per task
- **Task Count**: 1 instance per service (5 total)
- **Subnets**: Public subnets only (no NAT Gateway costs)
- **Logs**: 1-day retention
- **Storage**: Minimal RDS storage (20GB)
- **Backups**: Disabled to stay in Free Tier

### Security
- Security groups restrict database access to ECS tasks only
- Secrets Manager for database credentials
- IAM roles with least privilege access

## 🛠 Management Commands

### Scale Services
```bash
# Scale up/down (careful with Free Tier limits)
terraform apply -var="desired_count=2" -var-file="free-tier.tfvars"
```

### View Logs
```bash
# Via AWS CLI
aws logs tail /ecs/gateway-service --follow --region eu-central-1
```

### Clean Up
```bash
# Destroy everything to stop costs
terraform destroy -var-file="free-tier.tfvars" -auto-approve
```

## 🔍 Troubleshooting

### Common Issues

1. **ECS Tasks Not Starting**
   - Check CloudWatch logs for container errors
   - Verify ECR images are pushed and accessible
   - Ensure security groups allow required ports

2. **Database Connection Issues**
   - Verify RDS security group allows port 5432 from ECS tasks
   - Check Secrets Manager contains correct credentials

3. **Load Balancer Health Checks Failing**
   - Ensure gateway-service responds on `/actuator/health`
   - Check container port mapping (8080)

### Monitoring
- **ECS Service Events**: Real-time deployment status
- **CloudWatch Logs**: Application logs and errors
- **RDS Monitoring**: Database performance metrics

## 📚 Integration with Existing CI/CD

This Terraform setup integrates with your existing GitHub Actions workflow:

1. **Infrastructure First**: Deploy with Terraform
2. **Images Second**: Your existing CI/CD pushes to the created ECR repos
3. **Auto-Deploy**: ECS services automatically pull new images

## 🎯 Production Considerations

For production deployment (beyond Free Tier):
- Enable RDS backups and Multi-AZ
- Use private subnets with NAT Gateway
- Enable Container Insights monitoring
- Implement auto-scaling policies
- Add SSL/TLS termination at ALB
- Enable RDS encryption

## 📞 Support

If you encounter issues:
1. Check AWS Free Tier usage in Billing Console
2. Review CloudWatch logs for specific error messages
3. Verify IAM permissions for ECS task execution role

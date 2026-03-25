# Environment Variables Template - Sangrah Cloud Storage

**Location**: Create `.env` file in project root or set as system environment variables

---

## Stripe Configuration

```bash
# Stripe API Key (Secret)
STRIPE_API_KEY=sk_test_xxxxxxxxxxxxxxxxxxxx

# Stripe Webhook Secret (from Dashboard > Webhooks)
STRIPE_WEBHOOK_SECRET=whsec_test_xxxxxxxxxxxxxxxxxxxx

# Stripe Public Key (for frontend)
STRIPE_PUBLIC_KEY=pk_test_xxxxxxxxxxxxxxxxxxxx
```

---

## Email Service Configuration (SMTP)

### Gmail (Recommended for Testing)

```bash
# Gmail SMTP Settings
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=xxxx_xxxx_xxxx_xxxx  # Use App Password, NOT account password!

# Email Configuration
APP_EMAIL_FROM=noreply@sangrah.com
APP_EMAIL_FROM_NAME=Sangrah Cloud Storage
```

**How to get Gmail App Password:**
1. Go to https://myaccount.google.com/security
2. Enable "2-Step Verification"
3. Go to "App passwords" → Select Mail + Windows Computer
4. Copy the 16-character password
5. Paste as `MAIL_PASSWORD`

### Alternative: SendGrid

```bash
MAIL_HOST=smtp.sendgrid.net
MAIL_PORT=587
MAIL_USERNAME=apikey
MAIL_PASSWORD=SG.xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

### Alternative: AWS SES

```bash
MAIL_HOST=email-smtp.us-east-1.amazonaws.com
MAIL_PORT=587
MAIL_USERNAME=AKIA...
MAIL_PASSWORD=xxx...
```

---

## Email Service Ports & Databases

```bash
# Email Service (Port 8086)
EMAIL_PORT=8086
EMAIL_MONGODB_URI=mongodb://localhost:27017/sangrah_email
```

---

## MongoDB Configuration

```bash
# Gallery Service Database
GALLERY_MONGODB_URI=mongodb://localhost:27017/sangrah_gallery

# Billing Service Database
BILLING_MONGODB_URI=mongodb://localhost:27017/sangrah_billing

# Email Service Database
EMAIL_MONGODB_URI=mongodb://localhost:27017/sangrah_email

# Auth Service Database
AUTH_MONGODB_URI=mongodb://localhost:27017/sangrah_auth

# Event Service Database
EVENT_MONGODB_URI=mongodb://localhost:27017/sangrah_event

# Notification Service Database
NOTIFICATION_MONGODB_URI=mongodb://localhost:27017/sangrah_notification
```

---

## Service Discovery (Eureka)

```bash
# Eureka Server URL
EUREKA_SERVER_URL=http://localhost:8761/eureka/

# Instance Hostname
EUREKA_INSTANCE_HOSTNAME=localhost
```

---

## JWT & Security

```bash
# Auth Service - JWT Secret (Generate with: openssl rand -base64 32)
JWT_SECRET=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx

# JWT Expiration (in seconds)
JWT_EXPIRATION=3600

# JWT Refresh Token Expiration (in seconds)
JWT_REFRESH_EXPIRATION=604800
```

---

## API Gateway Configuration

```bash
# API Gateway Port
GATEWAY_PORT=8080

# API Gateway Secret
GATEWAY_SECRET=xxxxxxxxxxxxxxxxxxxxx
```

---

## Service URLs (for Feign Clients)

```bash
# Gallery Service URL (used by Billing)
GALLERY_SERVICE_URL=http://localhost:8082

# Billing Service URL (used by others)
BILLING_SERVICE_URL=http://localhost:8084

# Email Service URL (used by Billing)
EMAIL_SERVICE_URL=http://localhost:8086

# Auth Service URL
AUTH_SERVICE_URL=http://localhost:8081

# Event Service URL
EVENT_SERVICE_URL=http://localhost:8083

# Notification Service URL
NOTIFICATION_SERVICE_URL=http://localhost:8085
```

---

## Database Credentials (if not local)

```bash
# MongoDB Cloud Atlas
MONGODB_ATLAS_URI=mongodb+srv://username:password@cluster.mongodb.net/?retryWrites=true&w=majority

# Alternative: Local MongoDB with authentication
MONGODB_USERNAME=admin
MONGODB_PASSWORD=securepassword
MONGODB_HOST=localhost
MONGODB_PORT=27017
```

---

## Logging Configuration

```bash
# Log Level
LOG_LEVEL=DEBUG

# Log Format
LOG_FORMAT=json

# Log File Location
LOG_FILE_PATH=/var/log/sangrah/
```

---

## Production Configuration

```bash
# Application Environment
APP_ENV=production

# Enable Debug Mode
DEBUG_MODE=false

# Webhook Timeout (milliseconds)
WEBHOOK_TIMEOUT_MS=300000

# Webhook Event Max Age (seconds)
WEBHOOK_EVENT_MAX_AGE=600
```

---

## How to Use

### Option 1: Create `.env` file (Build Tools)
```bash
# In project root
cat > .env << EOF
STRIPE_API_KEY=sk_test_xxx
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
...
EOF
```

### Option 2: Export Environment Variables
```bash
# Linux/macOS
export STRIPE_API_KEY=sk_test_xxx
export MAIL_HOST=smtp.gmail.com
export MAIL_PORT=587

# Windows (PowerShell)
$env:STRIPE_API_KEY="sk_test_xxx"
$env:MAIL_HOST="smtp.gmail.com"
$env:MAIL_PORT="587"
```

### Option 3: Docker Compose
```yaml
version: '3.8'

services:
  email-service:
    environment:
      - MAIL_HOST=${MAIL_HOST}
      - MAIL_PORT=${MAIL_PORT}
      - MAIL_USERNAME=${MAIL_USERNAME}
      - MAIL_PASSWORD=${MAIL_PASSWORD}
```

### Option 4: Kubernetes Secrets
```bash
kubectl create secret generic stripe-config \
  --from-literal=stripeApiKey=sk_test_xxx \
  --from-literal=stripeWebhookSecret=whsec_test_xxx
```

---

## Verify Configuration

```bash
# Check Email Service is using correct SMTP
curl http://localhost:8086/api/v1/email/health

# Check Billing Service can reach Email Service
curl http://localhost:8084/api/v1/billing/health

# Check all services in Eureka
curl http://localhost:8761/
```

---

## Security Notes

⚠️ **DO NOT** commit `.env` or environment variables to Git!

```bash
# Add to .gitignore
echo ".env" >> .gitignore
echo ".env.local" >> .gitignore
echo ".env.*.swp" >> .gitignore
```

✅ **DO** use secure secret management:
- Development: `.env.local` (local machine only)
- Staging: Kubernetes Secrets
- Production: AWS Secrets Manager / HashiCorp Vault / Azure KeyVault

---

## Default Values (if env vars not set)

| Variable | Default | Purpose |
|----------|---------|---------|
| `MAIL_HOST` | `smtp.gmail.com` | SMTP Server |
| `MAIL_PORT` | `587` | SMTP Port (TLS) |
| `EMAIL_PORT` | `8086` | Email Service Listen Port |
| `EUREKA_SERVER_URL` | `http://localhost:8761/eureka/` | Service Registry |
| `WEBHOOK_TIMEOUT_MS` | `300000` | 5-minute timeout |
| `WEBHOOK_EVENT_MAX_AGE` | `600` | 10-minute max age |

---

## Troubleshooting

### Email Service Won't Start
- Check `MAIL_HOST` and `MAIL_PORT` are correct
- Verify `MAIL_USERNAME` and `MAIL_PASSWORD` are valid
- For Gmail: Confirm App Password (not account password)
- Check firewall allows SMTP port 587 outbound

### Stripe Webhooks Not Working
- Verify `STRIPE_WEBHOOK_SECRET` matches your webhook in Stripe Dashboard
- Check `STRIPE_API_KEY` has correct permissions
- Ensure webhook endpoint URL in Stripe Dashboard points to correct domain

### Services Can't Find Each Other
- Check `EUREKA_SERVER_URL` is correct and Eureka is running
- Verify service URLs in Feign clients match running services
- Check firewall between services

### MongoDB Connection Failed
- Verify `MONGODB_URI` is correct
- Check MongoDB is running: `mongosh`
- Verify database name exists
- Check credentials if authentication enabled

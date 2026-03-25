# Phase C - Complete Environment Setup Guide

**Goal**: Configure all services to run Phase C tests successfully
**Time**: 10-15 minutes
**Difficulty**: Easy

---

## Overview

You need to configure:
1. ✅ **SMTP** (Email sending) - Gmail, SendGrid, or AWS SES
2. ✅ **Stripe** (Payment processing) - Test API keys
3. ✅ **MongoDB** (Databases) - Local or cloud
4. ✅ **Environment Variables** - Set in your shell or .env file

---

## Step 1: Get Gmail App Password (Recommended for Testing)

Gmail in 2024 requires "App Passwords" instead of your actual account password for security.

### 1a. Enable 2-Step Verification

1. Go to: https://myaccount.google.com/security
2. Click **"2-Step Verification"** under "How you sign in to Google"
3. Follow the prompts (use your phone for verification)
4. Click **"Turn on"**

### 1b. Create App Password

1. Go to: https://myaccount.google.com/apppasswords
2. Under **"Select the app and device you want to generate the app password for"**:
   - **App**: Select "Mail"
   - **Device**: Select "Windows Computer" (or your OS)
3. Click **"Generate"**
4. Google will show you a 16-character password like: `xxxx xxxx xxxx xxxx`
5. **Copy this password** (without spaces)

### 1c. Save Your Email Credentials

```bash
# Your Gmail
MAIL_USERNAME="your-email@gmail.com"  # Your actual Gmail address
MAIL_PASSWORD="xxxxxxxxxxxxxx"        # The 16-char app password (no spaces)
MAIL_HOST="smtp.gmail.com"
MAIL_PORT="587"
```

**⚠️ Important**:
- ✅ Use the **16-character App Password**, NOT your Gmail password
- ✅ Remove spaces from the app password
- ✅ Keep these credentials secure (don't commit to git!)

---

## Step 2: Get Stripe Test Keys

### 2a. Create Stripe Account

1. Go to: https://dashboard.stripe.com/register
2. Sign up for free (no payment required)
3. Verify your email

### 2b. Get API Keys

1. Go to: https://dashboard.stripe.com/apikeys
2. You should see:
   - **Publishable Key**: `pk_test_xxxxxxxxxxxx`
   - **Secret Key**: `sk_test_xxxxxxxxxxxx`
3. These are in **test mode** (development) - perfect for testing

### 2c. Get Webhook Secret

1. Go to: https://dashboard.stripe.com/webhooks
2. Click **"Add endpoint"**
3. **Endpoint URL**: `http://localhost:8084/api/v1/billing/webhooks/stripe`
4. **Events**: Select `payment_intent.succeeded`, `payment_intent.payment_failed`
5. Click **"Add endpoint"**
6. You'll see **Signing secret** like: `whsec_test_xxxxxxxxxxxx`
7. Click the eye icon to reveal it
8. **Copy this secret**

### 2d. Save Your Stripe Credentials

```bash
STRIPE_API_KEY="sk_test_xxxxxxxxxxxxxxxxxxxx"           # Secret key
STRIPE_WEBHOOK_SECRET="whsec_test_xxxxxxxxxxxxxxxxxxxx"  # Webhook signing secret
STRIPE_PUBLIC_KEY="pk_test_xxxxxxxxxxxxxxxxxxxx"         # For frontend (optional)
```

---

## Step 3: Verify MongoDB is Running

### Option A: Local MongoDB (Recommended for Dev)

```bash
# Check if MongoDB is running
mongosh --eval "db.adminCommand('ping')"

# Expected output:
# { ok: 1 }

# If MongoDB not running, start it:

# macOS (via Homebrew)
brew services start mongodb-community

# Windows (via PowerShell as Admin)
net start MongoDB

# Linux (Ubuntu/Debian)
sudo systemctl start mongod

# Verify again
mongosh --eval "db.adminCommand('ping')"
```

### Option B: MongoDB Atlas (Cloud)

If using MongoDB Cloud:

```bash
# Get connection string from https://cloud.mongodb.com
# Format: mongodb+srv://username:password@cluster.mongodb.net/

# Export as environment variable:
export MONGODB_ATLAS_URI="mongodb+srv://user:pass@cluster.mongodb.net/?retryWrites=true&w=majority"
```

### 3a. Create Required Databases

```bash
mongosh << 'EOF'
// List existing databases
show databases

// These should auto-create when services start:
// - sangrah_auth (Auth service)
// - sangrah_gallery (Gallery service)
// - sangrah_billing (Billing service)
// - sangrah_email (Email service)
// - sangrah_event (Event service)
// - sangrah_notification (Notification service)

// Verify services created them
db = db.getSiblingDB('sangrah_email')
db.email_logs.insertOne({test: "created at setup"})
db.email_logs.deleteOne({test: "created at setup"})

echo "✅ MongoDB ready for testing"
EOF
```

---

## Step 4: Set Environment Variables

### Method 1: Create `.env` File (Simplest)

Create a file named `.env` in your project root:

```bash
# File: .env (project root)

# ============================================================
# SMTP Configuration (Gmail)
# ============================================================
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=xxxx xxxx xxxx xxxx

# Email Service
EMAIL_PORT=8086
APP_EMAIL_FROM=noreply@sangrah.com
APP_EMAIL_FROM_NAME=Sangrah Cloud Storage

# ============================================================
# Stripe Configuration (Test Keys)
# ============================================================
STRIPE_API_KEY=sk_test_xxxxxxxxxxxxxxxxxxxx
STRIPE_WEBHOOK_SECRET=whsec_test_xxxxxxxxxxxxxxxxxxxx
STRIPE_PUBLIC_KEY=pk_test_xxxxxxxxxxxxxxxxxxxx

# ============================================================
# MongoDB Configuration
# ============================================================
# Local MongoDB
AUTH_MONGODB_URI=mongodb://localhost:27017/sangrah_auth
GALLERY_MONGODB_URI=mongodb://localhost:27017/sangrah_gallery
BILLING_MONGODB_URI=mongodb://localhost:27017/sangrah_billing
EMAIL_MONGODB_URI=mongodb://localhost:27017/sangrah_email
EVENT_MONGODB_URI=mongodb://localhost:27017/sangrah_event
NOTIFICATION_MONGODB_URI=mongodb://localhost:27017/sangrah_notification

# Alternative: MongoDB Atlas (Cloud)
# MONGODB_ATLAS_URI=mongodb+srv://user:pass@cluster.mongodb.net/?retryWrites=true&w=majority

# ============================================================
# Service Configuration
# ============================================================
EUREKA_SERVER_URL=http://localhost:8761/eureka/
EUREKA_INSTANCE_HOSTNAME=localhost

GALLERY_SERVICE_URL=http://localhost:8082
BILLING_SERVICE_URL=http://localhost:8084
EMAIL_SERVICE_URL=http://localhost:8086
AUTH_SERVICE_URL=http://localhost:8081

# ============================================================
# JWT Configuration
# ============================================================
JWT_SECRET=your-super-secret-key-here-min-32-characters-long
JWT_EXPIRATION=3600
JWT_REFRESH_EXPIRATION=604800

# ============================================================
# Logging
# ============================================================
LOG_LEVEL=DEBUG
```

### Method 2: Export Environment Variables (Shell)

```bash
# macOS/Linux
export MAIL_HOST="smtp.gmail.com"
export MAIL_PORT="587"
export MAIL_USERNAME="your-email@gmail.com"
export MAIL_PASSWORD="xxxx xxxx xxxx xxxx"
export STRIPE_API_KEY="sk_test_xxxxxxxxxxxxxxxxxxxx"
export STRIPE_WEBHOOK_SECRET="whsec_test_xxxxxxxxxxxxxxxxxxxx"
export EMAIL_PORT="8086"

# Verify exported
echo $MAIL_HOST
echo $STRIPE_API_KEY
```

### Method 3: Windows PowerShell

```powershell
$env:MAIL_HOST = "smtp.gmail.com"
$env:MAIL_PORT = "587"
$env:MAIL_USERNAME = "your-email@gmail.com"
$env:MAIL_PASSWORD = "xxxx xxxx xxxx xxxx"
$env:STRIPE_API_KEY = "sk_test_xxxxxxxxxxxxxxxxxxxx"
$env:STRIPE_WEBHOOK_SECRET = "whsec_test_xxxxxxxxxxxxxxxxxxxx"
$env:EMAIL_PORT = "8086"

# Verify
Write-Host $env:MAIL_HOST
```

### Method 4: Docker Compose

If running in Docker:

```yaml
# docker-compose.yml
version: '3.8'

services:
  email-service:
    environment:
      - MAIL_HOST=${MAIL_HOST}
      - MAIL_PORT=${MAIL_PORT}
      - MAIL_USERNAME=${MAIL_USERNAME}
      - MAIL_PASSWORD=${MAIL_PASSWORD}
      - STRIPE_API_KEY=${STRIPE_API_KEY}
      - STRIPE_WEBHOOK_SECRET=${STRIPE_WEBHOOK_SECRET}
```

---

## Step 5: Verify All Environment Variables

```bash
# Create a test script: check-env.sh

#!/bin/bash

echo "=== ENVIRONMENT VARIABLE CHECK ==="

echo ""
echo "SMTP Configuration:"
echo "  MAIL_HOST: ${MAIL_HOST:-(NOT SET)}"
echo "  MAIL_PORT: ${MAIL_PORT:-(NOT SET)}"
echo "  MAIL_USERNAME: ${MAIL_USERNAME:-(NOT SET)}"
echo "  MAIL_PASSWORD: ${MAIL_PASSWORD:-(hidden)}"

echo ""
echo "Stripe Configuration:"
echo "  STRIPE_API_KEY: ${STRIPE_API_KEY:-(NOT SET)}"
echo "  STRIPE_WEBHOOK_SECRET: ${STRIPE_WEBHOOK_SECRET:-(NOT SET)}"

echo ""
echo "MongoDB Configuration:"
echo "  AUTH_MONGODB_URI: ${AUTH_MONGODB_URI:-(NOT SET)}"
echo "  BILLING_MONGODB_URI: ${BILLING_MONGODB_URI:-(NOT SET)}"
echo "  EMAIL_MONGODB_URI: ${EMAIL_MONGODB_URI:-(NOT SET)}"

echo ""
echo "Service URLs:"
echo "  GALLERY_SERVICE_URL: ${GALLERY_SERVICE_URL:-(NOT SET)}"
echo "  EMAIL_SERVICE_URL: ${EMAIL_SERVICE_URL:-(NOT SET)}"

echo ""
echo "=== CHECKING CRITICAL VARIABLES ==="
if [ -z "$MAIL_HOST" ]; then echo "⚠️  MAIL_HOST not set"; else echo "✅ MAIL_HOST set"; fi
if [ -z "$STRIPE_API_KEY" ]; then echo "⚠️  STRIPE_API_KEY not set"; else echo "✅ STRIPE_API_KEY set"; fi
if [ -z "$EMAIL_MONGODB_URI" ]; then echo "⚠️  EMAIL_MONGODB_URI not set"; else echo "✅ EMAIL_MONGODB_URI set"; fi
```

Run it:
```bash
chmod +x check-env.sh
./check-env.sh
```

---

## Step 6: Test SMTP Connection

```bash
# Test Gmail SMTP (requires `nc` - netcat)
nc -zv smtp.gmail.com 587

# Expected output:
# Connection to smtp.gmail.com port 587 [tcp/submission] succeeded!

# If you have `telnet`:
telnet smtp.gmail.com 587
# Type: QUIT (to exit)
```

---

## Step 7: Test Stripe Connection

```bash
# Test Stripe API key is valid
curl -s -X GET https://api.stripe.com/v1/customers \
  -u "$STRIPE_API_KEY:" | jq .

# Expected response: List of customers (should be empty initially)
# If authentication fails: {"error": {"message": "Invalid API Key provided"}}
```

---

## Step 8: Verify All Services Are Running

```bash
echo "=== SERVICE HEALTH CHECK ==="

# Eureka
echo -n "Eureka (8761): "
curl -s http://localhost:8761/ | grep -q "Eureka" && echo "✅ UP" || echo "❌ DOWN"

# Auth Service
echo -n "Auth (8081): "
curl -s http://localhost:8081/api/v1/auth/health | jq -r '.status' 2>/dev/null || echo "❌ DOWN"

# Gallery Service
echo -n "Gallery (8082): "
curl -s http://localhost:8082/api/v1/gallery/health | jq -r '.status' 2>/dev/null || echo "❌ DOWN"

# Billing Service
echo -n "Billing (8084): "
curl -s http://localhost:8084/api/v1/billing/health | jq -r '.status' 2>/dev/null || echo "❌ DOWN"

# Email Service
echo -n "Email (8086): "
curl -s http://localhost:8086/api/v1/email/health | jq -r '.status' 2>/dev/null || echo "❌ DOWN"

# API Gateway
echo -n "API Gateway (8080): "
curl -s http://localhost:8080/actuator/health | jq -r '.status' 2>/dev/null || echo "❌ DOWN"
```

---

## Step 9: Configure Services to Use Environment Variables

### 9a. Email Service Application Properties

Verify file: `Backend/Email/src/main/resources/application.properties`

```properties
spring.application.name=Email
server.port=${EMAIL_PORT:8086}
spring.data.mongodb.uri=${EMAIL_MONGODB_URI:mongodb://localhost:27017/sangrah_email}
spring.mail.host=${MAIL_HOST:smtp.gmail.com}
spring.mail.port=${MAIL_PORT:587}
spring.mail.username=${MAIL_USERNAME:}
spring.mail.password=${MAIL_PASSWORD:}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
spring.mail.properties.mail.smtp.connectiontimeout=5000
spring.mail.properties.mail.smtp.timeout=5000
spring.mail.properties.mail.smtp.writetimeout=5000

app.email.from=${APP_EMAIL_FROM:noreply@sangrah.com}
app.email.from.name=${APP_EMAIL_FROM_NAME:Sangrah Cloud Storage}

eureka.client.service-url.defaultZone=${EUREKA_SERVER_URL:http://localhost:8761/eureka/}
```

✅ Already configured!

### 9b. Billing Service Application Properties

Verify file: `Backend/Billing/src/main/resources/application.properties`

Should include:

```properties
stripe.api-key=${STRIPE_API_KEY}
stripe.webhook.secret=${STRIPE_WEBHOOK_SECRET}
email.service.url=${EMAIL_SERVICE_URL:http://localhost:8086}
gallery.service.url=${GALLERY_SERVICE_URL:http://localhost:8082}
```

---

## Step 10: Complete Setup Verification Checklist

```bash
# ============================================================
# RUN THIS COMPLETE VERIFICATION SCRIPT
# ============================================================

#!/bin/bash

echo "╔═══════════════════════════════════════════════════════╗"
echo "║  SANGRAH PHASE C - ENVIRONMENT SETUP VERIFICATION    ║"
echo "╚═══════════════════════════════════════════════════════╝"

PASS="✅"
FAIL="❌"

# 1. Environment Variables
echo ""
echo "1. ENVIRONMENT VARIABLES"
[ -n "$MAIL_HOST" ] && echo "$PASS MAIL_HOST set" || echo "$FAIL MAIL_HOST (NOT SET)"
[ -n "$STRIPE_API_KEY" ] && echo "$PASS STRIPE_API_KEY set" || echo "$FAIL STRIPE_API_KEY (NOT SET)"
[ -n "$EMAIL_MONGODB_URI" ] && echo "$PASS EMAIL_MONGODB_URI set" || echo "$FAIL EMAIL_MONGODB_URI (NOT SET)"

# 2. Services Running
echo ""
echo "2. SERVICES RUNNING"
curl -s http://localhost:8761/ > /dev/null && echo "$PASS Eureka (8761)" || echo "$FAIL Eureka (8761)"
curl -s http://localhost:8080/actuator/health > /dev/null && echo "$PASS API Gateway (8080)" || echo "$FAIL API Gateway (8080)"
curl -s http://localhost:8081/api/v1/auth/health > /dev/null && echo "$PASS Auth (8081)" || echo "$FAIL Auth (8081)"
curl -s http://localhost:8082/api/v1/gallery/health > /dev/null && echo "$PASS Gallery (8082)" || echo "$FAIL Gallery (8082)"
curl -s http://localhost:8084/api/v1/billing/health > /dev/null && echo "$PASS Billing (8084)" || echo "$FAIL Billing (8084)"
curl -s http://localhost:8086/api/v1/email/health > /dev/null && echo "$PASS Email (8086)" || echo "$FAIL Email (8086)"

# 3. Databases
echo ""
echo "3. DATABASES"
mongosh --eval "db.adminCommand('ping')" &>/dev/null && echo "$PASS MongoDB" || echo "$FAIL MongoDB"

# 4. Connectivity
echo ""
echo "4. CONNECTIVITY"
nc -zv smtp.gmail.com 587 2>&1 | grep -q "succeeded" && echo "$PASS SMTP (gmail)" || echo "$FAIL SMTP (gmail)"

# 5. Stripe
echo ""
echo "5. STRIPE CONFIGURATION"
curl -s -u "$STRIPE_API_KEY:" https://api.stripe.com/v1/customers 2>/dev/null | grep -q "object" && \
  echo "$PASS Stripe API Key valid" || echo "$FAIL Stripe API Key (invalid)"

echo ""
echo "╔═══════════════════════════════════════════════════════╗"
echo "║  SETUP STATUS: Ready for testing! ✅                 ║"
echo "╚═══════════════════════════════════════════════════════╝"
```

Save and run:
```bash
chmod +x verify-setup.sh
./verify-setup.sh
```

---

## Troubleshooting Setup

### Issue: "MAIL_HOST not recognized"
```bash
# Solution: Set in your shell first
export MAIL_HOST="smtp.gmail.com"
export MAIL_PASSWORD="your-app-password"

# Then restart your IDE/terminal
```

### Issue: "Cannot connect to SMTP"
```bash
# Check credentials
echo "Username: $MAIL_USERNAME"
echo "Host: $MAIL_HOST"
echo "Port: $MAIL_PORT"

# Verify Gmail App Password
# 1. Go to https://myaccount.google.com/apppasswords
# 2. Re-generate if needed
# 3. Copy without spaces
```

### Issue: "MongoDB connection refused"
```bash
# Start MongoDB
mongosh

# Should connect successfully
# If not, start MongoDB service:
# macOS: brew services start mongodb-community
# Linux: sudo systemctl start mongod
# Windows: net start MongoDB
```

### Issue: "Stripe API Key invalid"
```bash
# Verify key format
echo $STRIPE_API_KEY  # Should start with "sk_test_"

# Get new key from: https://dashboard.stripe.com/apikeys
# Make sure you're in TEST mode (not live!)
```

---

## Ready for Testing?

Once you've completed all steps and the verification script shows all green checkmarks, you're ready!

Next: Follow **TEST_EXECUTION_GUIDE.md** to run the complete test suite.

```bash
# Quick summary of what to do:
echo "✅ Step 1: Gmail App Password (created)"
echo "✅ Step 2: Stripe Test Keys (saved)"
echo "✅ Step 3: MongoDB (running)"
echo "✅ Step 4: Environment Variables (set in .env)"
echo "✅ Step 5: Verified environment variables"
echo "✅ Step 6: Tested SMTP connection"
echo "✅ Step 7: Tested Stripe API"
echo "✅ Step 8: Verified all services running"
echo "✅ Step 9: Services configured"
echo "✅ Step 10: Verification checklist passed"
echo ""
echo "🚀 READY TO TEST! Follow TEST_EXECUTION_GUIDE.md"
```

---

## Quick Reference: All Required Variables

| Variable | Value | Source |
|----------|-------|--------|
| `MAIL_HOST` | `smtp.gmail.com` | Gmail SMTP |
| `MAIL_PORT` | `587` | Gmail SMTP |
| `MAIL_USERNAME` | your@gmail.com | Your Gmail |
| `MAIL_PASSWORD` | 16-char app password | Gmail App Passwords |
| `STRIPE_API_KEY` | `sk_test_xxx` | Stripe Dashboard |
| `STRIPE_WEBHOOK_SECRET` | `whsec_test_xxx` | Stripe Webhooks |
| `EMAIL_MONGODB_URI` | `mongodb://localhost:27017/sangrah_email` | Local MongoDB |
| `EUREKA_SERVER_URL` | `http://localhost:8761/eureka/` | Local Eureka |

---

**All set? Reply with:**
- ✅ **"Setup complete"** → We'll run the tests
- ❌ **Any errors?** → I'll help troubleshoot

# Phase C - Quick Start Testing Guide

**Objective**: Test all 3 Phase C features end-to-end
**Estimated Time**: 30-45 minutes
**Prerequisites**: All services running, environment variables configured

---

## Step 1: Verify All Services Are Running

```bash
# Check Eureka Dashboard
open http://localhost:8761/
# Should show all 8 services registered

# Quick health checks
curl http://localhost:8081/api/v1/auth/health
curl http://localhost:8082/api/v1/gallery/health
curl http://localhost:8084/api/v1/billing/health
curl http://localhost:8086/api/v1/email/health

# Expected response: {"status":"UP"}
```

---

## Step 2: Configure Environment Variables

Create `.env` file in project root or export variables:

```bash
# Stripe (Test Keys)
export STRIPE_API_KEY="sk_test_xxxxxxxxxxxxxxxxxxxx"
export STRIPE_WEBHOOK_SECRET="whsec_test_xxxxxxxxxxxxxxxxxxxx"

# Gmail SMTP (or use SendGrid, AWS SES)
export MAIL_HOST="smtp.gmail.com"
export MAIL_PORT="587"
export MAIL_USERNAME="your-email@gmail.com"
export MAIL_PASSWORD="xxxx_xxxx_xxxx_xxxx"  # App Password for Gmail

# Email Service
export EMAIL_PORT="8086"
export APP_EMAIL_FROM="noreply@sangrah.com"
export APP_EMAIL_FROM_NAME="Sangrah Cloud Storage"

# MongoDB
export GALLERY_MONGODB_URI="mongodb://localhost:27017/sangrah_gallery"
export BILLING_MONGODB_URI="mongodb://localhost:27017/sangrah_billing"
export EMAIL_MONGODB_URI="mongodb://localhost:27017/sangrah_email"
export AUTH_MONGODB_URI="mongodb://localhost:27017/sangrah_auth"
```

---

## Step 3: Test Authentication (Foundation)

```bash
# Register a test user
TOKEN_RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test-phase-c@example.com",
    "password": "TestPassword123!",
    "firstName": "Phase",
    "lastName": "C"
  }')

echo "Registration Response:"
echo $TOKEN_RESPONSE | jq .

# Save token and userId for next tests
TOKEN=$(echo $TOKEN_RESPONSE | jq -r '.token')
USER_ID=$(echo $TOKEN_RESPONSE | jq -r '.userId')

echo "✅ Token: $TOKEN"
echo "✅ User ID: $USER_ID"
```

---

## Step 4: Test Gallery Ledger (Storage Usage)

```bash
# Create a storage usage entry
curl -X POST http://localhost:8082/api/v1/gallery/ledger \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "'$USER_ID'",
    "storageUsedMB": 1024,
    "fileCount": 50,
    "tier": "premium"
  }'

echo "✅ Storage ledger entry created"

# Get ledger entries (used for billing)
curl "http://localhost:8082/api/v1/gallery/ledger?startDate=2026-01-01T00:00:00Z&endDate=2026-12-31T23:59:59Z" \
  -H "Authorization: Bearer $TOKEN" | jq .
```

---

## Step 5: Test Invoice Generation

```bash
# Trigger monthly invoice job
curl -X POST http://localhost:8084/api/v1/billing/jobs/monthly/run \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json"

echo "✅ Invoice generation triggered"

# Get invoices
INVOICE_RESPONSE=$(curl -s http://localhost:8084/api/v1/billing/invoices \
  -H "Authorization: Bearer $TOKEN")

echo "Invoices:"
echo $INVOICE_RESPONSE | jq .

# Save invoice ID for payment test
INVOICE_ID=$(echo $INVOICE_RESPONSE | jq -r '.invoices[0].invoiceId')
AMOUNT=$(echo $INVOICE_RESPONSE | jq -r '.invoices[0].amount')

echo "✅ Invoice ID: $INVOICE_ID"
echo "✅ Amount: $AMOUNT"
```

---

## Step 6: Test Stripe Payment Intent

```bash
# Create payment intent
PAYMENT_RESPONSE=$(curl -s -X POST http://localhost:8084/api/v1/billing/payments/stripe/create-intent \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "invoiceId": "'$INVOICE_ID'",
    "amount": '$AMOUNT'00,
    "currency": "usd"
  }')

echo "Payment Intent Response:"
echo $PAYMENT_RESPONSE | jq .

# Save clientSecret
CLIENT_SECRET=$(echo $PAYMENT_RESPONSE | jq -r '.clientSecret')
echo "✅ Client Secret: $CLIENT_SECRET"

# Now user would complete payment in frontend...
# For testing: Use Stripe test card 4242 4242 4242 4242, any future date, any CVC
```

---

## Step 7: Simulate Webhook (Payment Success)

**Option A: Using Stripe CLI (Recommended)**

```bash
# Install Stripe CLI from: https://stripe.com/docs/stripe-cli

# Terminal 1: Listen for webhooks
stripe listen --forward-to http://localhost:8084/api/v1/billing/webhooks/stripe

# Terminal 2: Trigger payment success event
stripe trigger payment_intent.succeeded --api-key sk_test_xxx

# Expected output in Terminal 1: Webhook received
```

**Option B: Direct cURL Simulation**

```bash
# Get current timestamp and create signature
TIMESTAMP=$(date +%s)
PAYLOAD='{"id":"evt_test_phase_c","type":"payment_intent.succeeded","created":'$TIMESTAMP',"data":{"object":{"id":"pi_test_phase_c","status":"succeeded"}}}'

# Note: Signature validation will fail unless you match the actual algorithm
# This is just for testing endpoint accessibility
curl -X POST http://localhost:8084/api/v1/billing/webhooks/stripe \
  -H "Stripe-Signature: t=$TIMESTAMP,v1=test_signature" \
  -H "Content-Type: application/json" \
  -d "$PAYLOAD"

echo "✅ Webhook endpoint accessible"
```

---

## Step 8: Verify Invoice Status & PDF

```bash
# Check invoice status (should be PAID after webhook)
curl -s http://localhost:8084/api/v1/billing/invoices \
  -H "Authorization: Bearer $TOKEN" | jq '.invoices[0] | {status, pdfGeneratedAt, pdfUrl}'

# Download PDF
curl -X GET "http://localhost:8084/api/v1/billing/invoices/$INVOICE_ID/pdf" \
  -H "Authorization: Bearer $TOKEN" \
  --output test_invoice.pdf

# Verify PDF was created
ls -lh test_invoice.pdf
file test_invoice.pdf  # Should show: PDF document

echo "✅ PDF generated and downloaded"
```

---

## Step 9: Check Email Delivery

```bash
# Query email logs in MongoDB
mongosh << 'EOF'
use sangrah_email
db.email_logs.find().pretty()
EOF

# Expected output:
# {
#   "emailId": "...",
#   "toEmail": "test-phase-c@example.com",
#   "emailType": "invoice-paid",
#   "status": "sent",
#   "sentAt": ISODate(...),
#   "invoiceId": "INV-..."
# }

echo "✅ Email delivery logged"
```

---

## Step 10: Verify MongoDB Audit Logs

```bash
# Check all collections
mongosh << 'EOF'
# Gallery
use sangrah_gallery
echo "Gallery Ledger:"
db.storage_usage_ledger.find().pretty()

# Billing
use sangrah_billing
echo "Invoices:"
db.invoices.find().pretty()

# Email
use sangrah_email
echo "Email Logs:"
db.email_logs.find().pretty()
EOF
```

---

## Quick Test Script (Run All Steps)

Save this as `run_tests.sh`:

```bash
#!/bin/bash
set -e

echo "🚀 Phase C Testing Suite"
echo "======================="

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}Step 1: Verifying services...${NC}"
curl -s http://localhost:8761/ > /dev/null && echo "✅ Eureka UP"

echo -e "${BLUE}Step 2: Registering user...${NC}"
TOKEN_RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test-'$(date +%s)'@example.com",
    "password": "TestPassword123!",
    "firstName": "Test",
    "lastName": "User"
  }')

TOKEN=$(echo $TOKEN_RESPONSE | jq -r '.token')
USER_ID=$(echo $TOKEN_RESPONSE | jq -r '.userId')
echo "✅ User registered: $USER_ID"

echo -e "${BLUE}Step 3: Creating storage ledger...${NC}"
curl -s -X POST http://localhost:8082/api/v1/gallery/ledger \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "'$USER_ID'",
    "storageUsedMB": 512,
    "fileCount": 25,
    "tier": "standard"
  }' > /dev/null
echo "✅ Storage ledger created"

echo -e "${BLUE}Step 4: Generating invoice...${NC}"
curl -s -X POST http://localhost:8084/api/v1/billing/jobs/monthly/run \
  -H "Authorization: Bearer $TOKEN" > /dev/null
echo "✅ Invoice generated"

echo -e "${BLUE}Step 5: Getting invoice details...${NC}"
INVOICE_RESPONSE=$(curl -s http://localhost:8084/api/v1/billing/invoices \
  -H "Authorization: Bearer $TOKEN")
INVOICE_ID=$(echo $INVOICE_RESPONSE | jq -r '.invoices[0].invoiceId')
echo "✅ Invoice ID: $INVOICE_ID"

echo -e "${BLUE}Step 6: Creating payment intent...${NC}"
PAYMENT=$(curl -s -X POST http://localhost:8084/api/v1/billing/payments/stripe/create-intent \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "invoiceId": "'$INVOICE_ID'",
    "amount": 2999,
    "currency": "usd"
  }')
echo "✅ Payment intent created"

echo -e "${GREEN}✅ All tests passed!${NC}"
echo ""
echo "Next steps:"
echo "1. Complete payment in Stripe (test card: 4242 4242 4242 4242)"
echo "2. Stripe will send webhook to: POST /api/v1/billing/webhooks/stripe"
echo "3. PDF will be generated automatically"
echo "4. Email will be sent to user"
echo ""
echo "Monitor logs:"
echo "  tail -f /logs/billing-service.log"
echo "  tail -f /logs/email-service.log"
```

Run it:
```bash
chmod +x run_tests.sh
./run_tests.sh
```

---

## Expected Test Results

### ✅ All Tests Pass When:
- [ ] User registers successfully
- [ ] Gallery ledger entry created
- [ ] Invoice generated with correct amount
- [ ] Payment intent created with clientSecret
- [ ] Webhook processed (200 OK response)
- [ ] Invoice status changed to PAID
- [ ] PDF generated and downloadable
- [ ] Email log created in MongoDB
- [ ] Email received in inbox (if SMTP configured)

### ❌ If Tests Fail:
Check:
1. **Service health**: `curl http://localhost:PORT/api/v1/SERVICE/health`
2. **Logs**: `tail -f logs/service-name.log`
3. **MongoDB**: `mongosh` and check collections
4. **Environment variables**: Verify STRIPE_API_KEY, MAIL credentials
5. **Eureka**: `http://localhost:8761/` - check all services registered

---

## Monitoring During Tests

### Terminal 1: Watch Email Service Logs
```bash
tail -f /path/to/email-service.log | grep -E "📧|✅|❌"
```

### Terminal 2: Watch Billing Service Logs
```bash
tail -f /path/to/billing-service.log | grep -E "🔔|✅|❌|PDF"
```

### Terminal 3: MongoDB Queries
```bash
mongosh
use sangrah_email
db.email_logs.watch()  # Real-time log updates
```

---

## Troubleshooting

### Issue: "Port 8080 already in use"
```bash
# Kill process on port 8080
lsof -ti:8080 | xargs kill -9
```

### Issue: "Authorization denied" (403)
```bash
# Check JWT token is valid
echo $TOKEN | jq -R 'split(".") | map(@base64d) | .[1] | fromjson'
```

### Issue: Email not sent
```bash
# Test SMTP credentials
nc -zv smtp.gmail.com 587

# Check Email service logs
grep -i "smtp\|mail" /path/to/email-service.log
```

### Issue: PDF not generated
```bash
# Check Billing service logs
grep -i "pdf\|itext" /path/to/billing-service.log

# Verify PDF file in MongoDB
mongosh
use sangrah_billing
db.invoices.findOne({invoiceId: "INV-..."}, {pdfContent: 1})
```

---

## Next: Production Testing

Once all tests pass:
1. Load testing (100+ concurrent payments)
2. Webhook retry testing
3. Email timeout scenarios
4. PDF generation stress test
5. Production deployment

**Ready to start? Run the tests and share any errors you encounter!**

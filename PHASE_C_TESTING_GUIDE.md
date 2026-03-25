# Phase C Testing Guide - Complete Payment Flow

**Last Updated**: March 25, 2026
**Status**: ✅ Ready for Testing
**Services**: All running on designated ports

---

## Prerequisites

Before testing, ensure:

```bash
✅ Eureka Server (8761) - Running
✅ API Gateway (8080) - Running
✅ Auth Service (8081) - Running
✅ Gallery Service (8082) - Running
✅ Event Service (8083) - Running
✅ Billing Service (8084) - Running
✅ Notification Service (8085) - Running
✅ Email Service (8086) - Running
✅ MongoDB - Running (if using local: mongodb://localhost:27017)
✅ Stripe Account - Configured with webhook secret
```

---

## Setup: Environment Variables

Create `.env` file or set environment variables:

```bash
# Stripe Configuration
STRIPE_API_KEY=sk_test_xxxxxxxxxxxx
STRIPE_WEBHOOK_SECRET=whsec_xxxxxxxxxx

# Email Configuration (Gmail example)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password  # Use App Password, not account password

# Email Service
EMAIL_PORT=8086
EMAIL_MONGODB_URI=mongodb://localhost:27017/sangrah_email
APP_EMAIL_FROM=noreply@sangrah.com
APP_EMAIL_FROM_NAME=Sangrah Cloud Storage

# Optional: Eureka Server
EUREKA_SERVER_URL=http://localhost:8761/eureka/
```

---

## Test 1: Health Checks

### 1.1 All Services Health
```bash
# Auth Service
curl http://localhost:8081/api/v1/auth/health

# Gallery Service
curl http://localhost:8082/api/v1/gallery/health

# Billing Service
curl http://localhost:8084/api/v1/billing/health

# Email Service
curl http://localhost:8086/api/v1/email/health

# Eureka Registry
curl http://localhost:8761/
```

**Expected Response:**
```json
{"status": "UP"}
```

---

## Test 2: Authentication Flow

### 2.1 Register User
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "testuser@example.com",
    "password": "TestPassword123!",
    "firstName": "Test",
    "lastName": "User"
  }'
```

**Expected Response:**
```json
{
  "userId": "user123",
  "email": "testuser@example.com",
  "token": "eyJhbGc...",
  "message": "User registered successfully"
}
```

### 2.2 Login User
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "testuser@example.com",
    "password": "TestPassword123!"
  }'
```

**Save the `token` and `userId` for next tests**

---

## Test 3: Gallery Service - Storage Ledger

### 3.1 Create Storage Usage Entry
```bash
TOKEN="your-jwt-token"
USER_ID="your-user-id"

curl -X POST http://localhost:8082/api/v1/gallery/ledger \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "'$USER_ID'",
    "storageUsedMB": 1024,
    "fileCount": 50,
    "tier": "premium"
  }'
```

### 3.2 Get Ledger Entries (for billing)
```bash
curl "http://localhost:8082/api/v1/gallery/ledger?startDate=2026-03-01T00:00:00Z&endDate=2026-04-01T00:00:00Z" \
  -H "Authorization: Bearer $TOKEN"
```

**Expected Response:**
```json
[
  {
    "ledgerId": "ledger123",
    "userId": "user123",
    "storageUsedMB": 1024,
    "fileCount": 50,
    "recordedAt": "2026-03-25T22:30:00Z"
  }
]
```

---

## Test 4: Billing Service - Invoice Creation

### 4.1 Trigger Monthly Invoice Generation
```bash
curl -X POST http://localhost:8084/api/v1/billing/jobs/monthly/run \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json"
```

**Expected Response:**
```json
{
  "status": "success",
  "generatedCount": 1,
  "failureCount": 0,
  "timestamp": "2026-03-25T22:35:00Z"
}
```

### 4.2 Get User Invoices
```bash
curl http://localhost:8084/api/v1/billing/invoices \
  -H "Authorization: Bearer $TOKEN"
```

**Expected Response:**
```json
{
  "invoices": [
    {
      "invoiceId": "INV-2026-03-001",
      "userId": "user123",
      "amount": 29.99,
      "status": "PENDING",
      "dueDate": "2026-04-25",
      "createdAt": "2026-03-25T22:35:00Z",
      "paymentIntentId": null,
      "pdfUrl": null
    }
  ]
}
```

---

## Test 5: Stripe Payment Processing

### 5.1 Initialize Stripe Payment
```bash
INVOICE_ID="INV-2026-03-001"

curl -X POST http://localhost:8084/api/v1/billing/payments/stripe/create-intent \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "invoiceId": "'$INVOICE_ID'",
    "amount": 2999,
    "currency": "usd"
  }'
```

**Expected Response:**
```json
{
  "clientSecret": "pi_test_xxx_secret_xxx",
  "publishableKey": "pk_test_xxx",
  "amount": 2999,
  "currency": "usd"
}
```

### 5.2 Complete Payment (Frontend/Mobile)
- Use the `clientSecret` in your frontend Stripe Elements integration
- User enters test card: `4242 4242 4242 4242` with any future expiry
- Enter any CVC: `123`

**Test Cards (Stripe):**
```
SUCCESS:  4242 4242 4242 4242
DECLINE:  4000 0000 0000 0002
3D-SECURE: 4000 0025 0000 3155
```

---

## Test 6: Stripe Webhook Handling

### 6.1 Simulate Webhook Locally (Stripe CLI)

**Install Stripe CLI**: https://stripe.com/docs/stripe-cli

```bash
# Listen to webhooks
stripe listen --forward-to http://localhost:8084/api/v1/billing/webhooks/stripe

# In another terminal, trigger test event
stripe trigger payment_intent.succeeded
```

### 6.2 Direct Webhook Test (cURL)

```bash
WEBHOOK_SECRET="whsec_test_secret"
PAYLOAD='{"id":"evt_test","type":"payment_intent.succeeded","data":{"object":{"id":"pi_test","status":"succeeded"}}}'
TIMESTAMP=$(date +%s)
SIGNATURE="t=$TIMESTAMP,v1=test_signature"

curl -X POST http://localhost:8084/api/v1/billing/webhooks/stripe \
  -H "Stripe-Signature: $SIGNATURE" \
  -H "Content-Type: application/json" \
  -d "$PAYLOAD"
```

**Expected Response:**
```json
{
  "status": "success",
  "eventId": "evt_test",
  "eventType": "payment_intent.succeeded",
  "processingTime_ms": 250
}
```

---

## Test 7: PDF Generation Verification

### 7.1 Check Invoice Status (after payment)
```bash
curl http://localhost:8084/api/v1/billing/invoices \
  -H "Authorization: Bearer $TOKEN"
```

**Expected Response:**
```json
{
  "invoices": [
    {
      "status": "PAID",
      "pdfGeneratedAt": "2026-03-25T22:40:00Z",
      "pdfUrl": "/api/v1/billing/invoices/INV-2026-03-001/pdf"
    }
  ]
}
```

### 7.2 Download Invoice PDF
```bash
INVOICE_ID="INV-2026-03-001"

curl -X GET http://localhost:8084/api/v1/billing/invoices/$INVOICE_ID/pdf \
  -H "Authorization: Bearer $TOKEN" \
  --output invoice.pdf

# View the PDF
open invoice.pdf  # macOS
start invoice.pdf # Windows
```

**Expected:**
- PDF file with invoice details
- Professional formatting with Helvetica fonts
- Invoice number, amount, date, line items
- File size: 50-150 KB

---

## Test 8: Email Service Testing

### 8.1 Send Test Email Directly
```bash
curl -X POST http://localhost:8086/api/v1/email/invoices/paid \
  -H "Content-Type: application/json" \
  -d '{
    "invoiceId": "INV-2026-03-001",
    "userId": "user123",
    "userEmail": "testuser@example.com",
    "amount": 29.99,
    "pdfContent": null
  }'
```

**Expected Response:**
```json
{
  "status": "sent",
  "emailId": "email_uuid",
  "timestamp": "2026-03-25T22:45:00Z",
  "message": "Payment confirmation email sent successfully"
}
```

### 8.2 Check Email Delivery Logs
```bash
# Via MongoDB
use sangrah_email
db.email_logs.find().pretty()

# Expected document:
{
  "_id": ObjectId(...),
  "emailId": "email_uuid",
  "toEmail": "testuser@example.com",
  "emailType": "invoice-paid",
  "status": "sent",
  "sentAt": ISODate("2026-03-25T22:45:00Z"),
  "invoiceId": "INV-2026-03-001"
}
```

### 8.3 Verify Email Received
- Check Gmail/email client for message from `noreply@sangrah.com`
- Subject: `Payment Confirmation - Invoice INV-2026-03-001`
- Verify PDF attachment is present
- Check responsive design on mobile

---

## Test 9: Complete End-to-End Flow

### Sequence:
```
1. User registers & logs in
   ↓
2. Uploads files to Gallery (creates storage ledger)
   ↓
3. Monthly billing job runs (creates invoice)
   ↓
4. User initiates Stripe payment
   ↓
5. Card processing completes
   ↓
6. Webhook received: payment_intent.succeeded
   ↓
7. Invoice marked as PAID
   ↓
8. PDF generated via iText7
   ↓
9. Email Service called via Feign
   ↓
10. Email sent with PDF attachment
   ↓
11. Audit log created in sangrah_email
```

### Full Test Script:
```bash
#!/bin/bash

# 1. Register & Login
REGISTER=$(curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "e2e-test@example.com",
    "password": "TestPass123!",
    "firstName": "E2E",
    "lastName": "Test"
  }')

TOKEN=$(echo $REGISTER | jq -r '.token')
USER_ID=$(echo $REGISTER | jq -r '.userId')

echo "✅ User registered: $USER_ID"
echo "✅ Token: $TOKEN"

# 2. Create billing entry
curl -s -X POST http://localhost:8082/api/v1/gallery/ledger \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "'$USER_ID'",
    "storageUsedMB": 512,
    "fileCount": 25,
    "tier": "standard"
  }'

echo "✅ Storage ledger created"

# 3. Generate invoice
INVOICE=$(curl -s -X POST http://localhost:8084/api/v1/billing/jobs/monthly/run \
  -H "Authorization: Bearer $TOKEN")

echo "✅ Invoice generated: $INVOICE"

# 4. Simulate payment webhook
curl -s -X POST http://localhost:8084/api/v1/billing/webhooks/stripe \
  -H "Stripe-Signature: t=1234567890,v1=test_sig" \
  -H "Content-Type: application/json" \
  -d '{
    "id": "evt_test",
    "type": "payment_intent.succeeded",
    "data": {"object": {"id": "pi_test", "status": "succeeded"}}
  }'

echo "✅ Webhook processed"

# 5. Check PDF
sleep 2
curl -s -X GET http://localhost:8084/api/v1/billing/invoices \
  -H "Authorization: Bearer $TOKEN" | jq '.invoices[0].status'

echo "✅ Payment flow completed!"
```

---

## Test 10: Error Scenarios

### 10.1 Invalid Stripe Signature
```bash
curl -X POST http://localhost:8084/api/v1/billing/webhooks/stripe \
  -H "Stripe-Signature: t=1234567890,v1=invalid_sig" \
  -H "Content-Type: application/json" \
  -d '{"id":"evt_test"}'
```

**Expected Response:** `403 Forbidden`

### 10.2 Email Service Timeout (SMTP)
Set `MAIL_HOST` to invalid hostname → Email service should:
- Log error
- Return status: "failed"
- NOT block invoice processing
- Create audit log with failure reason

### 10.3 PDF Generation Failure
Corrupt PDF settings → Email service should:
- Gracefully fall back
- Send email without PDF
- Log failure in MongoDB

---

## Monitoring During Tests

### MongoDB Collections to Monitor:
```bash
# Invoices
use sangrah_billing
db.invoices.find().pretty()

# Payments
db.payments.find().pretty()

# Email logs
use sangrah_email
db.email_logs.find().pretty()

# Storage ledger
use sangrah_gallery
db.storage_usage_ledger.find().pretty()
```

### Check Service Logs:
```bash
# Billing Service
tail -f /logs/billing-service.log

# Email Service
tail -f /logs/email-service.log

# Gateway
tail -f /logs/api-gateway.log
```

---

## Performance Benchmarks

| Operation | Expected Time | Threshold |
|-----------|---|---|
| Invoice Generation | < 500ms | 1s |
| PDF Generation | < 2s | 5s |
| Email Sending (SMTP) | < 3s | 5s |
| Webhook Processing | < 1s | 5s (total timeout) |
| Complete Flow | < 10s | 15s |

---

## Debugging Checklist

- [ ] All services showing UP in Eureka (http://localhost:8761)
- [ ] Billing service MongoDB contains new invoices
- [ ] Email service MongoDB contains email_logs
- [ ] Stripe webhook secret matches in application.properties
- [ ] SMTP credentials are correct (test with mail client first)
- [ ] Email templates rendering correctly (check Thymeleaf logs)
- [ ] PDF generated with correct invoice data
- [ ] Email sent with PDF attachment
- [ ] Feign client successfully calls Email service
- [ ] Event publishing and listening working correctly

---

## Next Steps After Testing

1. ✅ **Unit Tests** - Add JUnit tests for service methods
2. ✅ **Integration Tests** - Test inter-service communication
3. ✅ **Load Testing** - Verify 5-minute timeout strategy
4. ✅ **Production Deployment** - Configure ExternalSecret, ConfigMap for k8s
5. ✅ **Monitoring** - Set up ELK/Prometheus for production
6. ✅ **Feature Enhancements** - RabbitMQ for async emails (Phase D)

---

## Success Criteria

✅ All tests pass
✅ Payment flow completes without errors
✅ PDF generated after payment
✅ Email sent with PDF attachment
✅ Audit logs created in MongoDB
✅ Services communicate via REST successfully
✅ Webhooks processed correctly
✅ Error handling graceful (no stuck states)

**Report any failures to the development team with logs and error messages.**

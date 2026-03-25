# Phase C - Test Execution Walkthrough

**Status**: Ready to test all Phase C features
**Services**: All 8 running and healthy
**Timeline**: 15-20 minutes for complete flow

---

## ⚠️ Prerequisites Check

Before starting, verify:

```bash
# 1. All services are running
curl -s http://localhost:8761/ | grep -q "Eureka" && echo "✅ Eureka UP" || echo "❌ Eureka DOWN"
curl -s http://localhost:8081/api/v1/auth/health | jq -r '.status'
curl -s http://localhost:8082/api/v1/gallery/health | jq -r '.status'
curl -s http://localhost:8084/api/v1/billing/health | jq -r '.status'
curl -s http://localhost:8086/api/v1/email/health | jq -r '.status'

# 2. MongoDB is running
mongosh --eval "db.adminCommand('ping')" &>/dev/null && echo "✅ MongoDB UP" || echo "❌ MongoDB DOWN"

# 3. Environment variables are set
echo "STRIPE_API_KEY: ${STRIPE_API_KEY:-(NOT SET)}"
echo "MAIL_HOST: ${MAIL_HOST:-(NOT SET)}"
```

---

## Test 1: User Registration & Authentication

**Purpose**: Create test user and get JWT token for subsequent requests

```bash
# ============================================================
# EXECUTE THIS IN YOUR TERMINAL
# ============================================================

# Register new user
REGISTER_RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "phase-c-test@example.com",
    "password": "TestPassword123!",
    "firstName": "Phase",
    "lastName": "CTest"
  }')

echo "=== REGISTER RESPONSE ==="
echo $REGISTER_RESPONSE | jq .

# Extract token and userId
TOKEN=$(echo $REGISTER_RESPONSE | jq -r '.token')
USER_ID=$(echo $REGISTER_RESPONSE | jq -r '.userId')

echo ""
echo "✅ Extracted Values:"
echo "   TOKEN: ${TOKEN:0:50}..."
echo "   USER_ID: $USER_ID"

# Save for next steps
export TOKEN=$TOKEN
export USER_ID=$USER_ID
```

**Expected Output:**
```json
{
  "userId": "user-uuid-here",
  "email": "phase-c-test@example.com",
  "token": "eyJhbGc...",
  "message": "User registered successfully"
}
```

**✅ Success**: If you see a token and userId

---

## Test 2: Create Storage Ledger Entry

**Purpose**: Simulate user uploading files (creates billing data)

```bash
# ============================================================
# EXECUTE THIS IN YOUR TERMINAL (uses $TOKEN from Test 1)
# ============================================================

LEDGER_RESPONSE=$(curl -s -X POST http://localhost:8082/api/v1/gallery/ledger \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "'$USER_ID'",
    "storageUsedMB": 1024,
    "fileCount": 50,
    "tier": "premium"
  }')

echo "=== LEDGER CREATION RESPONSE ==="
echo $LEDGER_RESPONSE | jq .

# Verify ledger was created
LEDGER_ID=$(echo $LEDGER_RESPONSE | jq -r '.ledgerId')
echo ""
echo "✅ Storage ledger created:"
echo "   LEDGER_ID: $LEDGER_ID"
```

**Expected Output:**
```json
{
  "ledgerId": "ledger-uuid",
  "userId": "user-uuid",
  "storageUsedMB": 1024,
  "fileCount": 50,
  "recordedAt": "2026-03-25T22:35:00Z"
}
```

**✅ Success**: If you see ledgerId and recordedAt timestamp

---

## Test 3: Generate Monthly Invoice

**Purpose**: Trigger invoice creation (uses storage ledger for calculations)

```bash
# ============================================================
# EXECUTE THIS IN YOUR TERMINAL
# ============================================================

INVOICE_JOB=$(curl -s -X POST http://localhost:8084/api/v1/billing/jobs/monthly/run \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json")

echo "=== INVOICE JOB RESPONSE ==="
echo $INVOICE_JOB | jq .

# Wait 1 second for processing
sleep 1

# Fetch created invoices
INVOICES=$(curl -s http://localhost:8084/api/v1/billing/invoices \
  -H "Authorization: Bearer $TOKEN")

echo ""
echo "=== USER INVOICES ==="
echo $INVOICES | jq '.invoices | .[]'

# Extract invoice details
INVOICE_ID=$(echo $INVOICES | jq -r '.invoices[0].invoiceId')
AMOUNT=$(echo $INVOICES | jq -r '.invoices[0].amount')
STATUS=$(echo $INVOICES | jq -r '.invoices[0].status')

echo ""
echo "✅ Invoice Details:"
echo "   INVOICE_ID: $INVOICE_ID"
echo "   AMOUNT: \$$AMOUNT"
echo "   STATUS: $STATUS"

# Save for next steps
export INVOICE_ID=$INVOICE_ID
export AMOUNT=$AMOUNT
```

**Expected Output:**
```json
{
  "status": "success",
  "generatedCount": 1,
  "failureCount": 0,
  "timestamp": "2026-03-25T22:35:00Z"
}
```

**✅ Success**: If INVOICE_ID is created and STATUS is "PENDING"

---

## Test 4: Create Stripe Payment Intent

**Purpose**: Prepare for payment processing

```bash
# ============================================================
# EXECUTE THIS IN YOUR TERMINAL
# ============================================================

AMOUNT_CENTS=$((${AMOUNT%.*} * 100))  # Convert to cents

PAYMENT_INTENT=$(curl -s -X POST http://localhost:8084/api/v1/billing/payments/stripe/create-intent \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "invoiceId": "'$INVOICE_ID'",
    "amount": '$AMOUNT_CENTS',
    "currency": "usd"
  }')

echo "=== PAYMENT INTENT RESPONSE ==="
echo $PAYMENT_INTENT | jq .

# Extract clientSecret
CLIENT_SECRET=$(echo $PAYMENT_INTENT | jq -r '.clientSecret')
PUBLISHABLE_KEY=$(echo $PAYMENT_INTENT | jq -r '.publishableKey')

echo ""
echo "✅ Payment Intent Created:"
echo "   CLIENT_SECRET: ${CLIENT_SECRET:0:50}..."
echo "   PUBLISHABLE_KEY: ${PUBLISHABLE_KEY:0:50}..."

# Save for webhook simulation
export CLIENT_SECRET=$CLIENT_SECRET
```

**Expected Output:**
```json
{
  "clientSecret": "pi_test_xxxxx_secret_xxxxx",
  "publishableKey": "pk_test_xxxxx",
  "amount": 2999,
  "currency": "usd"
}
```

**✅ Success**: If you get clientSecret and publishableKey

---

## Test 5: Simulate Payment Success via Webhook

**Purpose**: Simulate Stripe webhook callback (this triggers PDF + Email)

### Option A: Using Stripe CLI (Recommended)

```bash
# ============================================================
# TERMINAL 1: Start listening for webhooks
# ============================================================

stripe listen --forward-to http://localhost:8084/api/v1/billing/webhooks/stripe

# You should see output like:
# > Ready! Your webhook signing secret is whsec_test_xxx

# Copy the signing secret and save it
export STRIPE_WEBHOOK_SECRET="whsec_test_xxx"


# ============================================================
# TERMINAL 2: Trigger payment success event
# ============================================================

stripe trigger payment_intent.succeeded --api-key $STRIPE_API_KEY

# You should see:
# > Sending test.event_request.created
# >
# > In Terminal 1, you should see:
# > 2026-03-25 22:40:00 [event_request] payment_intent.succeeded

# In Terminal 1, you'll see the webhook received:
# ✅ Output: {"received":true}
```

### Option B: Manual Webhook Simulation (if Stripe CLI not available)

```bash
# ============================================================
# EXECUTE THIS IN YOUR TERMINAL
# ============================================================

# Create a realistic webhook payload
TIMESTAMP=$(date +%s)

WEBHOOK_PAYLOAD=$(cat <<EOF
{
  "id": "evt_$(date +%s%N | cut -b1-13)",
  "type": "payment_intent.succeeded",
  "created": $TIMESTAMP,
  "data": {
    "object": {
      "id": "pi_test_phase_c",
      "client_secret": "$CLIENT_SECRET",
      "status": "succeeded",
      "amount": ${AMOUNT_CENTS},
      "currency": "usd"
    }
  }
}
EOF
)

echo "=== SENDING WEBHOOK ==="
echo $WEBHOOK_PAYLOAD | jq .

WEBHOOK_RESPONSE=$(curl -s -X POST http://localhost:8084/api/v1/billing/webhooks/stripe \
  -H "Stripe-Signature: t=$TIMESTAMP,v1=test_signature" \
  -H "Content-Type: application/json" \
  -d "$WEBHOOK_PAYLOAD")

echo ""
echo "=== WEBHOOK RESPONSE ==="
echo $WEBHOOK_RESPONSE | jq .

echo ""
echo "✅ Webhook processed"
```

**Expected Output:**
```json
{
  "status": "success",
  "eventId": "evt_xxx",
  "eventType": "payment_intent.succeeded",
  "processingTime_ms": 250
}
```

**✅ Success**: If status is "success" and processing time is under 5 minutes (5000ms)

---

## Test 6: Verify Invoice Status Changed to PAID

**Purpose**: Confirm webhook updated invoice status

```bash
# ============================================================
# EXECUTE THIS IN YOUR TERMINAL (wait 2 seconds first)
# ============================================================

sleep 2

UPDATED_INVOICES=$(curl -s http://localhost:8084/api/v1/billing/invoices \
  -H "Authorization: Bearer $TOKEN")

echo "=== UPDATED INVOICE STATUS ==="
echo $UPDATED_INVOICES | jq '.invoices[0] | {status, paidDate, pdfGeneratedAt}'

# Extract status
NEW_STATUS=$(echo $UPDATED_INVOICES | jq -r '.invoices[0].status')
PDF_GENERATED_AT=$(echo $UPDATED_INVOICES | jq -r '.invoices[0].pdfGeneratedAt')

echo ""
echo "✅ Invoice Updated:"
echo "   STATUS: $NEW_STATUS"
echo "   PDF_GENERATED_AT: $PDF_GENERATED_AT"

if [ "$NEW_STATUS" = "PAID" ]; then
  echo "   ✅ PAID status confirmed!"
else
  echo "   ⚠️ Status is still $NEW_STATUS (PDF may still be generating)"
fi
```

**Expected Output:**
```json
{
  "status": "PAID",
  "paidDate": "2026-03-25T22:40:00Z",
  "pdfGeneratedAt": "2026-03-25T22:40:02Z"
}
```

**✅ Success**: If status is "PAID" and pdfGeneratedAt is set

---

## Test 7: Download & Verify PDF Invoice

**Purpose**: Confirm PDF was generated and is downloadable

```bash
# ============================================================
# EXECUTE THIS IN YOUR TERMINAL
# ============================================================

echo "=== DOWNLOADING PDF ==="
curl -X GET "http://localhost:8084/api/v1/billing/invoices/$INVOICE_ID/pdf" \
  -H "Authorization: Bearer $TOKEN" \
  --output phase-c-test-invoice.pdf

echo ""
# Verify PDF file
if [ -f phase-c-test-invoice.pdf ]; then
  FILE_SIZE=$(ls -lh phase-c-test-invoice.pdf | awk '{print $5}')
  echo "✅ PDF Downloaded Successfully"
  echo "   FILE: phase-c-test-invoice.pdf"
  echo "   SIZE: $FILE_SIZE"

  # Check if it's a valid PDF
  file phase-c-test-invoice.pdf
else
  echo "❌ PDF download failed"
fi
```

**Expected Output:**
```
✅ PDF Downloaded Successfully
   FILE: phase-c-test-invoice.pdf
   SIZE: 85K
phase-c-test-invoice.pdf: PDF document, version 1.4
```

**✅ Success**: If file size is 50-150 KB and file type is PDF

---

## Test 8: Check Email Delivery Logs

**Purpose**: Verify email was sent and logged

```bash
# ============================================================
# EXECUTE THIS IN YOUR TERMINAL
# ============================================================

echo "=== EMAIL LOGS IN MONGODB ==="

mongosh << 'EOF'
use sangrah_email

// Get all email logs for this test
db.email_logs.find({toEmail: "phase-c-test@example.com"}).pretty()

// Count by status
echo "\n=== EMAIL LOG STATS ==="
db.email_logs.aggregate([
  {$match: {toEmail: "phase-c-test@example.com"}},
  {$group: {_id: "$emailType", count: {$sum: 1}}}
]).pretty()
EOF
```

**Expected Output:**
```json
{
  "_id": ObjectId("..."),
  "emailId": "email-uuid",
  "toEmail": "phase-c-test@example.com",
  "fromEmail": "noreply@sangrah.com",
  "emailType": "invoice-paid",
  "status": "sent",
  "sentAt": ISODate("2026-03-25T22:40:02.000Z"),
  "invoiceId": "INV-2026-03-001",
  "fromEmail": "noreply@sangrah.com"
}
```

**✅ Success**: If you see emailType "invoice-paid" with status "sent"

---

## Test 9: Check Email Actually Received

**Purpose**: Verify email was delivered to mailbox

```bash
# Check your email inbox
# Expected from: noreply@sangrah.com
# Expected subject: "Payment Confirmation - Invoice INV-xxx"
# Expected: Invoice details + PDF attachment

echo "⏱️  Waiting for email to arrive..."
echo "   Check your inbox for email from: noreply@sangrah.com"
echo "   Subject should be: 'Payment Confirmation - Invoice $INVOICE_ID'"
echo ""
echo "If email doesn't arrive:"
echo "   1. Check spam/junk folder"
echo "   2. Verify SMTP credentials in environment variables"
echo "   3. Check Email service logs: grep 'invoice-paid' email-service.log"
```

---

## Test 10: Full Database Verification

**Purpose**: Confirm all data is consistent across services

```bash
# ============================================================
# EXECUTE THIS IN YOUR TERMINAL
# ============================================================

mongosh << 'EOF'
echo "=== STORAGE LEDGER (Gallery) ==="
use sangrah_gallery
db.storage_usage_ledger.findOne({userId: "USER_ID_HERE"})

echo "\n=== INVOICES (Billing) ==="
use sangrah_billing
db.invoices.findOne({invoiceId: /INV-/})

echo "\n=== EMAIL LOGS (Email Service) ==="
use sangrah_email
db.email_logs.findOne({emailType: "invoice-paid"})

echo "\n=== VERIFICATION SUMMARY ==="
// Count documents in each collection
db.email_logs.countDocuments()
EOF
```

---

## Complete Test Summary

### Checklist: All Tests Passed ✅

- [ ] **Test 1**: User registered, token obtained
- [ ] **Test 2**: Storage ledger entry created
- [ ] **Test 3**: Invoice generated (PENDING status)
- [ ] **Test 4**: Payment intent created (clientSecret received)
- [ ] **Test 5**: Webhook processed (success response)
- [ ] **Test 6**: Invoice status changed to PAID
- [ ] **Test 7**: PDF downloaded (50-150 KB)
- [ ] **Test 8**: Email logged in MongoDB (status: sent)
- [ ] **Test 9**: Email received in inbox with PDF attachment
- [ ] **Test 10**: All data consistent across databases

---

## If Tests Fail

### Issue: "Service not found" (404)
```bash
# Check Eureka
curl http://localhost:8761/
# Verify service is registered
```

### Issue: "Authorization denied" (401/403)
```bash
# Verify token is valid
echo $TOKEN | jq -R 'split(".") | map(@base64d) | .[1] | fromjson'

# Check token expiration
echo $TOKEN | jq -R 'split(".") | .[1]' | base64 -d | jq '.exp'
```

### Issue: "Email not sent"
```bash
# Check Email service logs
tail -f /path/to/email-service.log | grep -i "mail\|smtp\|invoice-paid"

# Verify SMTP credentials
nc -zv smtp.gmail.com 587
```

### Issue: "PDF not generated"
```bash
# Check Billing service logs
tail -f /path/to/billing-service.log | grep -i "pdf\|invoice-paid"

# Verify PDF in MongoDB
mongosh
use sangrah_billing
db.invoices.findOne({invoiceId: "INV-..."}, {pdfContent: 1})
```

---

## Performance Summary

If all tests pass, you should see:

| Component | Expected Time | Status |
|-----------|---|---|
| User registration | < 500ms | ✅ |
| Ledger creation | < 200ms | ✅ |
| Invoice generation | < 1s | ✅ |
| Payment intent | < 500ms | ✅ |
| Webhook processing | < 1s | ✅ |
| PDF generation | < 2s | ✅ |
| Email delivery | < 3s | ✅ |
| **Total flow** | **< 10s** | ✅ |

---

## Next Steps (After Tests Pass)

1. ✅ Run load tests (100+ concurrent users)
2. ✅ Test webhook retries (simulate network failure)
3. ✅ Test email timeout (disable SMTP, verify error handling)
4. ✅ Commit code to Git
5. ✅ Plan Phase D enhancements (RabbitMQ, monitoring)

---

**Ready? Copy-paste the commands above and execute them in order!**

**Report any errors and I'll help you troubleshoot.** 🚀

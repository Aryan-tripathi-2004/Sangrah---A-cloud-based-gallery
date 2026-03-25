#!/bin/bash

# ==============================================================================
# DEBUG SCRIPT: Check Email and PDF Issues
# ==============================================================================
# Copy and paste this entire block into your terminal

echo "╔════════════════════════════════════════════════════════════╗"
echo "║     DEBUGGING EMAIL & PDF ISSUES                          ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""

# ============================================================================
# STEP 1: Check Email Logs in MongoDB
# ============================================================================
echo "STEP 1: Checking Email Logs"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

mongosh << 'EMAIL_CHECK'
use sangrah_email

print("\n📧 Email logs for aryantripathi1606@gmail.com:")
print("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

var emailCount = db.email_logs.countDocuments({toEmail: "aryantripathi1606@gmail.com"})
print("Total emails found: " + emailCount)

if (emailCount > 0) {
  print("\n✅ Email logs exist! Details:")
  db.email_logs.find({toEmail: "aryantripathi1606@gmail.com"}).pretty()
} else {
  print("\n❌ No email logs found!")
  print("   This means email sending was NOT attempted")
  print("   Possible reasons:")
  print("   1. Invoice payment event not triggered")
  print("   2. Email service not called by Billing service")
  print("   3. Event listener not working")
}
EMAIL_CHECK

echo ""

# ============================================================================
# STEP 2: Check Invoice PDF Status
# ============================================================================
echo "STEP 2: Checking Invoice PDF Status"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

mongosh << 'PDF_CHECK'
use sangrah_billing

print("\n📄 Invoice status for aryantripathi1606@gmail.com:")
print("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

var invoice = db.invoices.findOne({userEmail: "aryantripathi1606@gmail.com"})

if (invoice) {
  print("\n✅ Invoice found!")
  print("   Invoice ID: " + invoice.invoiceId)
  print("   Status: " + invoice.status)
  print("   Paid Date: " + invoice.paidDate)
  print("   PDF Generated At: " + invoice.pdfGeneratedAt)

  if (invoice.pdfContent) {
    print("   PDF Content Size: " + invoice.pdfContent.length + " bytes")
    print("   ✅ PDF GENERATED")
  } else {
    print("   PDF Content: NULL")
    print("   ❌ PDF NOT GENERATED")
    print("   Reason: pdfContent is empty/null")
  }

  print("\nFull invoice details:")
  invoice
} else {
  print("❌ No invoice found for this email!")
}
PDF_CHECK

echo ""

# ============================================================================
# STEP 3: Summary of Issues
# ============================================================================
echo "STEP 3: Issue Analysis"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "If no email logs found:"
echo "  → Issue: Event listener not triggering email send"
echo "  → Fix: Check InvoicePaidEventListener in Billing service"
echo ""
echo "If PDF is NULL:"
echo "  → Issue: PDF not generated after payment"
echo "  → Fix: Check PDFGenerationService in Billing service"
echo ""
echo "If email exists but not received:"
echo "  → Check Gmail spam folder"
echo "  → Verify SMTP credentials in .env"
echo "  → Check email status in logs (sent/failed)"
echo ""

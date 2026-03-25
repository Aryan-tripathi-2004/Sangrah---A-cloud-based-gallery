# 🔐 Secure Stripe Configuration Setup Guide

## What We Created

We've set up a secure system to store your Stripe keys without risking accidental commits to GitHub.

### Files Created:

```
g:\Sangrah - A Cloud Based Storage\
├── .gitignore                          ← Prevent .env files from being committed
├── Backend\Billing\.env                ← YOUR SECRET KEYS (LOCAL ONLY)
├── Backend\Billing\.env.example        ← Template to commit to git
├── frontend\.env                       ← Frontend config (LOCAL ONLY)
└── frontend\.env.example               ← Template to commit to git
```

---

## Step 1: Get Your Stripe Test Keys

### From Stripe Dashboard:

1. **Go to**: https://dashboard.stripe.com
2. **Login** with your account (sign up if needed)
3. **Make sure you're in TEST mode** (toggle at top left)
4. **Navigate to**: Developers → API Keys
5. **Copy your keys**:
   - **Publishable Key** (starts with `pk_test_`) → Goes in frontend
   - **Secret Key** (starts with `sk_test_`) → Goes in backend

### Get Webhook Secret:

1. **Navigate to**: Developers → Webhooks
2. **Click**: "Add endpoint"
3. **For local testing**, you'll use Stripe CLI (see Step 4)
4. **For production**, configure your webhook URL: `https://yourdomain.com/api/v1/billing/payments/webhook`

---

## Step 2: Fill in Backend .env File

**File**: `Backend\Billing\.env`

```env
# Replace these values with your actual Stripe test keys
STRIPE_API_KEY=sk_test_YOUR_SECRET_KEY_HERE
STRIPE_WEBHOOK_SECRET=whsec_test_YOUR_WEBHOOK_SECRET_HERE

# Leave these as default for local development
BILLING_MONGODB_URI=mongodb://localhost:27017/sangrah_billing
ENABLE_BATCH_JOBS=true
SCHEDULING_ENABLED=true
LOGGING_LEVEL=DEBUG
```

**Example with real keys** (don't actually commit):
```env
STRIPE_API_KEY=sk_test_4eC39HqLyjWDarhtT657j8e2
STRIPE_WEBHOOK_SECRET=whsec_test_1234567890abcdefgh
BILLING_MONGODB_URI=mongodb://localhost:27017/sangrah_billing
```

---

## Step 3: Fill in Frontend .env File

**File**: `frontend\.env`

```env
# Replace with your Stripe PUBLISHABLE key
NG_APP_STRIPE_PUBLIC_KEY=pk_test_YOUR_PUBLISHABLE_KEY_HERE

# For local development
NG_APP_API_URL=http://localhost:8080
NG_APP_ENVIRONMENT=development
```

**Example** (publishable keys are safe to share):
```env
NG_APP_STRIPE_PUBLIC_KEY=pk_test_4eC39HqLyjWDaRN2qEsyDe3v
NG_APP_API_URL=http://localhost:8080
NG_APP_ENVIRONMENT=development
```

---

## Step 4: Extract Webhook Secret (Optional for Local Testing)

If you want to test webhooks locally:

```bash
# Install Stripe CLI (one-time)
npm install -g @stripe/cli
# or download: https://stripe.com/docs/stripe-cli

# Login to Stripe
stripe login

# Listen for webhooks
stripe listen --api-key sk_test_YOUR_SECRET_KEY

# Output will show:
# Ready! Your webhook signing secret is: whsec_test_1234567890abcdefgh
```

Copy that `whsec_test_...` value and add it to your `.env` file.

---

## Step 5: Update Frontend environment.ts

Your frontend's `environment.ts` should now read from the `.env` file:

**File**: `frontend\src\environments\environment.ts`

```typescript
export const environment = {
  production: false,
  apiUrl: process.env['NG_APP_API_URL'] || 'http://localhost:8080',
  stripePublicKey: process.env['NG_APP_STRIPE_PUBLIC_KEY'] || 'pk_test_',
};
```

---

## Step 6: Verify .gitignore is Protecting Your Files

Check that `.env` files are protected:

```bash
# These files should be ignored by git
git status

# Should NOT show:
# Backend/Billing/.env
# frontend/.env
```

**What IS committed** (safe to share):
- ✅ `.env.example` files (templates only, no secrets)
- ✅ `application.properties` (references variables, no hardcoded values)

**What is NOT committed** (protected):
- ❌ `.env` files (your actual secrets)
- ❌ Any file with `STRIPE_API_KEY` or `STRIPE_WEBHOOK_SECRET` values

---

## Step 7: Start Your Services with Environment Variables

### Backend (Spring Boot)

**Option A: Load from .env file**
```bash
cd Backend/Billing

# On Windows (PowerShell)
$env:STRIPE_API_KEY = "sk_test_..."
$env:STRIPE_WEBHOOK_SECRET = "whsec_test_..."
mvn spring-boot:run

# On Mac/Linux
export STRIPE_API_KEY="sk_test_..."
export STRIPE_WEBHOOK_SECRET="whsec_test_..."
mvn spring-boot:run
```

**Option B: Automatic loading with Spring Boot**
Add to `Backend/Billing/.env`:
```
STRIPE_API_KEY=sk_test_YOUR_KEY
STRIPE_WEBHOOK_SECRET=whsec_test_YOUR_SECRET
```

Then run:
```bash
mvn spring-boot:run
```

Spring automatically loads from classpath `.env` if you have `dotenv` support.

### Frontend (Angular)

```bash
cd frontend

# Install dependencies
npm install

# Start dev server
npm start

# Your .env file will be loaded automatically
# Check environment via: console.log(environment)
```

---

## 🧪 Test Cards for Stripe

Once configured, test your payment flow:

| Scenario | Card Number | Expiry | CVC |
|----------|------------|--------|-----|
| ✅ Success | `4242 4242 4242 4242` | 12/25 | 123 |
| ❌ Declined | `4000 0000 0000 0002` | 12/25 | 123 |
| 🔒 3D Secure | `4000 0025 0000 3155` | 12/25 | 123 |

---

## 🔒 Security Checklist

- [ ] `.env` file is in `.gitignore` (won't be committed)
- [ ] `.env.example` has no real secret values
- [ ] Backend reads `stripe.api.key` from `${STRIPE_API_KEY}` env var
- [ ] Frontend reads `stripePublicKey` from `NG_APP_STRIPE_PUBLIC_KEY`
- [ ] No hardcoded keys in source files
- [ ] `.env` files are in `.gitignore` and won't appear in `git status`

---

## ❌ Common Mistakes to Avoid

| ❌ DON'T DO | ✅ DO THIS INSTEAD |
|-----------|-------------------|
| Hardcode `sk_test_...` in code | Use environment variable `${STRIPE_API_KEY}` |
| Commit `.env` file to git | Commit `.env.example` template only |
| Share secret key with frontend | Only share publishable key `pk_test_...` |
| Use production keys in development | Always use test keys for local work |
| Store keys in comments or docs | Store in `.env` file only |

---

## 📋 What's Next?

1. **Get Stripe keys** from dashboard
2. **Fill in `.env` files** with your test keys
3. **Verify `.gitignore`** protects the files
4. **Test the payment flow** with test card numbers
5. **Use webhook testing** with Stripe CLI (optional)

---

## 🆘 Troubleshooting

### "Stripe API key not found"
- Check that `STRIPE_API_KEY` is set in your `.env` file
- Restart your backend service after changing `.env`
- Verify `.env` file is in `Backend/Billing/` directory

### "java.lang.IllegalStateException: Stripe key not configured"
- Make sure your Spring application loads the `.env` file
- Add this to `pom.xml` if not already present:
  ```xml
  <dependency>
    <groupId>me.paulschwarz</groupId>
    <artifactId>spring-dotenv</artifactId>
    <version>4.0.0</version>
  </dependency>
  ```
- Then add to `application.properties`:
  ```properties
  spring.config.import=file:.env
  ```

### "publishable key is not a valid publishable key"
- Check that `NG_APP_STRIPE_PUBLIC_KEY` starts with `pk_test_`
- Make sure you used the PUBLISHABLE key, not the SECRET key
- Don't accidentally swap keys between frontend and backend

---

## 📞 Still Need Help?

Your files are now secure:
- 🔐 Backend secrets in `Backend/Billing/.env` (protected by `.gitignore`)
- 🔐 Frontend config in `frontend/.env` (protected by `.gitignore`)
- ✅ Safe templates in `.env.example` files (can be committed)
- ✅ Root `.gitignore` prevents all `.env` files from being committed

You're all set! Just fill in your keys and start developing! 🚀

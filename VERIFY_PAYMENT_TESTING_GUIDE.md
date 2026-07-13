# Testing Verify Payment API - Complete Guide

**Scenario:** Frontend performs Razorpay payment → Gets payment ID & signature → Calls verify endpoint

---

## 🔐 How Signature Works

### Signature Generation (What Frontend Does)

In production:
1. Razorpay returns `paymentId` to frontend
2. Frontend creates data: `orderId|paymentId`
3. Frontend generates signature: `HMAC-SHA256(data, razorpay_secret)`
4. Frontend sends all 3 to backend verify endpoint

### Signature Verification (What Backend Does)

Backend:
1. Receives: `orderId`, `paymentId`, `signature` from frontend
2. Creates same data: `orderId|paymentId`
3. Generates signature using same secret
4. Compares with received signature
5. If match → Payment is valid ✅

---

## 🧮 How to Generate Signature for Testing

### Step 1: Get Your Secret

The secret is in `application.properties`:
```properties
razorpay.key.secret=Y8X46wl0J2G7gxZvbzWcSDB9
```

### Step 2: Generate Fake Payment ID

Any format works for testing (must match what backend returns in verify):
```bash
# Option A: Simple format
pay_abc123xyz

# Option B: Random hex
pay_$(openssl rand -hex 6)

# Example output: pay_a1b2c3d4e5f6
```

### Step 3: Create Data String

Format: `{orderId}|{paymentId}`

Example:
```
orderId: order_1MHjR7rGRo8Ykw
paymentId: pay_a1b2c3d4e5f6

data = "order_1MHjR7rGRo8Ykw|pay_a1b2c3d4e5f6"
```

### Step 4: Generate HMAC-SHA256 Signature

#### Using OpenSSL (macOS/Linux)
```bash
echo -n "order_1MHjR7rGRo8Ykw|pay_a1b2c3d4e5f6" | \
  openssl dgst -sha256 -hmac "Y8X46wl0J2G7gxZvbzWcSDB9" -hex

# Output: (stdin)= 9ef4dffbfd84f1318f6739a3ce19f9d85851857ae648f114332d8401e0949a3d
# Copy the hash without "(stdin)= " prefix
```

#### Using Node.js
```javascript
const crypto = require('crypto');

const orderId = 'order_1MHjR7rGRo8Ykw';
const paymentId = 'pay_a1b2c3d4e5f6';
const secret = 'Y8X46wl0J2G7gxZvbzWcSDB9';

const data = orderId + '|' + paymentId;
const signature = crypto
  .createHmac('sha256', secret)
  .update(data)
  .digest('hex');

console.log(signature);
// Output: 9ef4dffbfd84f1318f6739a3ce19f9d85851857ae648f114332d8401e0949a3d
```

#### Using Python
```python
import hmac
import hashlib

orderId = 'order_1MHjR7rGRo8Ykw'
paymentId = 'pay_a1b2c3d4e5f6'
secret = 'Y8X46wl0J2G7gxZvbzWcSDB9'

data = f"{orderId}|{paymentId}"
signature = hmac.new(
    secret.encode(),
    data.encode(),
    hashlib.sha256
).hexdigest()

print(signature)
# Output: 9ef4dffbfd84f1318f6739a3ce19f9d85851857ae648f114332d8401e0949a3d
```

---

## 📝 Testing Workflow

### Complete Flow for Testing

```
Step 1: OTP Verify (POST /auth/otp/verify)
   ↓ [Get token]
   
Step 2: Get Plans (GET /subscription/plans/active)
   ↓ [Select plan]
   
Step 3: Create Order (POST /payments/create-order)
   ↓ [Get orderId]
   
Step 4: Generate Fake Payment Data
   - Generate paymentId
   - Generate signature
   
Step 5: Verify Payment (POST /payments/verify) ← Testing this
   ↓ [Call with fake data]
   
Step 6: Verify Results
   - Check status code 200
   - Check response message
   - Check database records
```

---

## 🧪 Testing Methods

### Method 1: Postman (Easiest)

The collection already has auto-scripts to generate signature!

**Steps:**
1. Run requests 1-3 as before (auth, plans, create order)
2. Request 4 "Verify Payment" has pre-request script that:
   - Generates fake paymentId
   - Computes HMAC-SHA256 signature
   - Sets environment variables
3. Click "Send"
4. Verify response

**Pre-request Script (Already in Collection):**
```javascript
// Automatically runs before request
var orderId = pm.environment.get('orderId');
if (!orderId) { throw new Error('orderId not set. Run Create Order first'); }

// Generate fake payment ID
var paymentId = 'pay_' + Math.random().toString(36).substring(2, 12);
pm.environment.set('paymentId', paymentId);

// Get secret from environment
var secret = pm.environment.get('razorpay_secret');
if (!secret) { throw new Error('razorpay_secret not set in environment'); }

// Create data string
var data = orderId + '|' + paymentId;

// Generate signature (Postman has CryptoJS built-in)
var signature = CryptoJS.HmacSHA256(data, secret).toString();
pm.environment.set('razorpaySignature', signature);

console.log('✓ Generated signature for', data);
```

---

### Method 2: Manual Testing with cURL + OpenSSL

**Complete Manual Workflow:**

```bash
#!/bin/bash

BASE_URL="http://localhost:8085"
PHONE="+911234567890"
OTP="123456"
RAZORPAY_SECRET="Y8X46wl0J2G7gxZvbzWcSDB9"

# Step 1: OTP Verify
echo "1️⃣ OTP Verify"
STEP1=$(curl -s -X POST "$BASE_URL/api/v1/auth/otp/verify" \
  -H "Content-Type: application/json" \
  -d "{\"phone\":\"$PHONE\",\"otp\":\"$OTP\"}")

AUTH_TOKEN=$(echo "$STEP1" | jq -r '.token')
echo "Token: $AUTH_TOKEN"

# Step 2: Get Plans
echo ""
echo "2️⃣ Get Plans"
STEP2=$(curl -s -X GET "$BASE_URL/api/v1/subscription/plans/active")
PLAN_ID=$(echo "$STEP2" | jq -r '.[0].id')
echo "Plan: $PLAN_ID"

# Step 3: Create Order
echo ""
echo "3️⃣ Create Order"
STEP3=$(curl -s -X POST "$BASE_URL/api/v1/payments/create-order" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $AUTH_TOKEN" \
  -d "{\"planId\":\"$PLAN_ID\"}")

ORDER_ID=$(echo "$STEP3" | jq -r '.orderId')
echo "Order ID: $ORDER_ID"

# Step 4: Generate Fake Payment Data
echo ""
echo "4️⃣ Generate Payment Data"
PAYMENT_ID="pay_$(openssl rand -hex 6)"
echo "Payment ID: $PAYMENT_ID"

# Step 5: Generate Signature
DATA_TO_SIGN="$ORDER_ID|$PAYMENT_ID"
echo "Data to sign: $DATA_TO_SIGN"

SIGNATURE=$(echo -n "$DATA_TO_SIGN" | \
  openssl dgst -sha256 -hmac "$RAZORPAY_SECRET" -hex | \
  sed 's/^.* //')

echo "Signature: $SIGNATURE"

# Step 6: Verify Payment
echo ""
echo "5️⃣ Verify Payment"
STEP5=$(curl -s -X POST "$BASE_URL/api/v1/payments/verify" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $AUTH_TOKEN" \
  -d "{
    \"planId\":\"$PLAN_ID\",
    \"razorpayOrderId\":\"$ORDER_ID\",
    \"razorpayPaymentId\":\"$PAYMENT_ID\",
    \"razorpaySignature\":\"$SIGNATURE\"
  }")

echo "Response: $STEP5"

if echo "$STEP5" | grep -q "Payment verified successfully"; then
  echo "✅ Payment verified!"
else
  echo "❌ Payment verification failed"
fi
```

**Run it:**
```bash
bash test-verify-payment.sh
```

---

### Method 3: Manual cURL (Step by Step)

**1. Get Token**
```bash
curl -X POST http://localhost:8085/api/v1/auth/otp/verify \
  -H "Content-Type: application/json" \
  -d '{"phone":"+911234567890","otp":"123456"}' | jq .

# Save: token and userId
```

**2. Get Plans**
```bash
curl -X GET http://localhost:8085/api/v1/subscription/plans/active | jq .

# Save: planId from first plan
```

**3. Create Order**
```bash
curl -X POST http://localhost:8085/api/v1/payments/create-order \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -d '{"planId":"plan_basic_monthly"}' | jq .

# Save: orderId
```

**4. Generate Signature (in terminal)**
```bash
# Replace with your actual values
ORDER_ID="order_1MHjR7rGRo8Ykw"
PAYMENT_ID="pay_abc123xyz"
SECRET="Y8X46wl0J2G7gxZvbzWcSDB9"

echo -n "${ORDER_ID}|${PAYMENT_ID}" | \
  openssl dgst -sha256 -hmac "$SECRET" -hex | \
  sed 's/^.* //'

# Copy output → this is your signature
```

**5. Verify Payment**
```bash
curl -X POST http://localhost:8085/api/v1/payments/verify \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -d '{
    "planId":"plan_basic_monthly",
    "razorpayOrderId":"order_1MHjR7rGRo8Ykw",
    "razorpayPaymentId":"pay_abc123xyz",
    "razorpaySignature":"YOUR_GENERATED_SIGNATURE_HERE"
  }' | jq .

# Expected: "Payment verified successfully"
```

---

## 🧬 Testing Scenarios

### ✅ Scenario 1: Valid Signature

**Input:**
```json
{
  "planId": "plan_basic_monthly",
  "razorpayOrderId": "order_1MHjR7rGRo8Ykw",
  "razorpayPaymentId": "pay_abc123xyz",
  "razorpaySignature": "9ef4dffbfd84f1318f6739a3ce19f9d85851857ae648f114332d8401e0949a3d"
}
```

**Expected Response:**
```
Status: 200
Body: "Payment verified successfully"
Database: Payment record marked SUCCESS, Subscription created
```

---

### ❌ Scenario 2: Invalid Signature

**Input:**
```json
{
  "planId": "plan_basic_monthly",
  "razorpayOrderId": "order_1MHjR7rGRo8Ykw",
  "razorpayPaymentId": "pay_abc123xyz",
  "razorpaySignature": "WRONG_SIGNATURE_HERE"
}
```

**Expected Response:**
```
Status: 400
Body: "Invalid signature"
Database: No changes (payment rejected)
```

---

### ❌ Scenario 3: Duplicate Payment

**Input:** Same `razorpayPaymentId` twice (to test idempotency)

**First Call:**
```
Status: 200
Body: "Payment verified successfully"
Database: Subscription created
```

**Second Call (same paymentId):**
```
Status: 200
Body: "Payment verified successfully"
Database: NO duplicate subscription (auto-prevented)
```

---

### ❌ Scenario 4: Invalid Plan

**Input:**
```json
{
  "planId": "non_existent_plan",
  ...
}
```

**Expected Response:**
```
Status: 400
Body: "Plan not found" or error message
```

---

## 🛠️ Signature Generation Tools

### Online Tools (for quick testing)

1. **HMAC-SHA256 Online Generator**
   - Go to: https://www.online-toolz.com/tools/hmac-generator
   - Algorithm: SHA256
   - Message: `{orderId}|{paymentId}`
   - Key: `Y8X46wl0J2G7gxZvbzWcSDB9`
   - Get: Hex output

2. **Postman (Built-in)**
   - Pre-request script has CryptoJS
   - Already generates signature automatically
   - See script above

### CLI Tools (Command Line)

**macOS/Linux:**
```bash
echo -n "data_to_sign" | openssl dgst -sha256 -hmac "secret" -hex
```

**Windows (with OpenSSL):**
```bash
echo -n "data_to_sign" | openssl dgst -sha256 -hmac "secret" -hex
```

**Node.js (Any Platform):**
```bash
node -e "
const crypto = require('crypto');
const msg = 'order_id|payment_id';
const key = 'secret';
console.log(crypto.createHmac('sha256', key).update(msg).digest('hex'));
"
```

---

## 📊 Complete Testing Checklist

### Before Testing Verify Payment

- [ ] Spring Boot app running on `http://localhost:8085`
- [ ] Database running and migrations applied
- [ ] Razorpay credentials in `application.properties`
- [ ] Know your `razorpay.key.secret` value

### Testing Verify Payment Endpoint

- [ ] Get valid auth token (OTP verify works)
- [ ] Create order successfully (get orderId)
- [ ] Generate valid fake paymentId
- [ ] Generate signature correctly
- [ ] Call verify with all 4 parameters
- [ ] Get 200 response "Payment verified successfully"
- [ ] Check database: Payment record created with SUCCESS
- [ ] Check database: Subscription created with ACTIVE status

### Testing Error Cases

- [ ] Invalid signature → 400 error
- [ ] Duplicate paymentId → Should prevent duplicate subscription
- [ ] Invalid orderId → Should fail
- [ ] Invalid planId → Should fail
- [ ] Missing auth token → 401 error

---

## 🔍 Debugging Tips

### If You Get "Invalid Signature"

**Checklist:**
1. ✓ Correct orderId from create-order response?
2. ✓ Correct paymentId (any format works)?
3. ✓ Correct secret from application.properties?
4. ✓ Correct format: `orderId|paymentId` (with pipe)?
5. ✓ Using SHA256 (not MD5 or other)?
6. ✓ Output is hex string (64 characters)?

**Debug Command:**
```bash
# Verify your signature generation
orderId="order_1MHjR7rGRo8Ykw"
paymentId="pay_abc123"
secret="Y8X46wl0J2G7gxZvbzWcSDB9"

# Print what you're signing
echo "Signing: ${orderId}|${paymentId}"

# Generate signature
signature=$(echo -n "${orderId}|${paymentId}" | \
  openssl dgst -sha256 -hmac "$secret" -hex | \
  sed 's/^.* //')

echo "Signature: $signature"

# This signature should match what backend expects
```

---

## 🎯 Real Frontend Implementation

When building the frontend, here's how it works:

```javascript
// After Razorpay payment succeeds, you get:
const paymentData = {
  razorpay_payment_id: "pay_1MHjR7rGRo8Ykw",    // From Razorpay
  razorpay_order_id: "order_1MHjR7rGRo8Ykw",    // From Razorpay
  razorpay_signature: "signature_from_razorpay" // From Razorpay
};

// Send to backend
const response = await fetch('/api/v1/payments/verify', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${authToken}`
  },
  body: JSON.stringify({
    planId: selectedPlanId,
    razorpayOrderId: paymentData.razorpay_order_id,
    razorpayPaymentId: paymentData.razorpay_payment_id,
    razorpaySignature: paymentData.razorpay_signature
  })
});

// Handle response
if (response.ok) {
  console.log('✅ Payment verified!');
  // Subscription is now active
} else {
  console.log('❌ Payment verification failed');
  // Show error to user
}
```

---

## 📚 Reference

### Endpoint Details
```
POST /api/v1/payments/verify

Headers:
- Content-Type: application/json
- Authorization: Bearer {token}

Body:
{
  "planId": "string",
  "razorpayOrderId": "string",
  "razorpayPaymentId": "string",
  "razorpaySignature": "string"
}

Response:
- 200: "Payment verified successfully"
- 400: "Invalid signature" or plan not found error
- 401: Unauthorized (missing/invalid token)
```

### Signature Format
```
Data to Sign: orderId|paymentId
Algorithm: HMAC-SHA256
Key: razorpay_secret from application.properties
Output: Hex string (64 characters)
```

---

## 🎉 Summary

### To Test Verify Payment:

1. **Get orderId** → Create order endpoint
2. **Generate fake paymentId** → Any format like `pay_abc123`
3. **Generate signature** → HMAC-SHA256 of `orderId|paymentId`
4. **Call verify endpoint** → POST with all 4 parameters
5. **Verify response** → 200 + success message

### Quick Test Methods:
- **Postman** (easiest) — Auto-scripts generate signature
- **Bash script** — `test-verify-payment.sh`
- **Manual cURL** — Copy-paste friendly

### Remember:
- Frontend generates signature in real scenario
- For testing, you generate fake signature
- Signature = HMAC-SHA256(orderId|paymentId, secret)
- Same logic runs on both frontend and backend



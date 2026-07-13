# Payment API - Quick Reference Payloads

## Environment Variables Setup

```json
{
  "baseUrl": "http://localhost:8085",
  "phone": "+911234567890",
  "otp": "123456",
  "authToken": "(saved from Step 1)",
  "userId": "(saved from Step 1)",
  "planId": "(saved from Step 2)",
  "orderId": "(saved from Step 3)",
  "orderAmount": "(saved from Step 3)",
  "paymentId": "(generated in Step 4)",
  "razorpaySignature": "(computed in Step 4)",
  "razorpay_secret": "Y8X46wl0J2G7gxZvbzWcSDB9"
}
```

---

## Step 1: OTP Verify

### Request
```bash
curl -X POST http://localhost:8085/api/v1/auth/otp/verify \
  -H "Content-Type: application/json" \
  -d '{
    "phone": "+911234567890",
    "otp": "123456"
  }'
```

### Expected Response
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userId": "550e8400-e29b-41d4-a716-446655440000"
}
```

---

## Step 2: Get Active Plans

### Request
```bash
curl -X GET http://localhost:8085/api/v1/subscription/plans/active
```

### Expected Response
```json
[
  {
    "id": "plan_basic_monthly",
    "name": "Basic Monthly",
    "description": "1 month basic plan",
    "price": 499,
    "currency": "INR",
    "durationDays": 30
  },
  {
    "id": "plan_premium_yearly",
    "name": "Premium Yearly",
    "description": "12 months premium plan",
    "price": 4999,
    "currency": "INR",
    "durationDays": 365
  }
]
```

---

## Step 3: Create Order

### Request
```bash
curl -X POST http://localhost:8085/api/v1/payments/create-order \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -d '{
    "planId": "plan_basic_monthly"
  }'
```

### Payload (JSON)
```json
{
  "planId": "plan_basic_monthly"
}
```

### Expected Response
```json
{
  "orderId": "order_1MHjR7rGRo8Ykw",
  "amount": 49900,
  "currency": "INR",
  "keyId": "rzp_live_1a2b3c4d5e6f7g8h"
}
```

### Response Breakdown:
- **orderId**: Razorpay order ID (use in Step 4)
- **amount**: 49900 paise = ₹499
- **currency**: INR (Indian Rupees)
- **keyId**: Razorpay public key (for frontend)

---

## Step 4: Verify Payment

### How to Generate Signature

1. Get the values:
   ```
   orderId = "order_1MHjR7rGRo8Ykw"
   paymentId = "pay_ABC123XYZ" (simulated/from Razorpay)
   razorpay_secret = "Y8X46wl0J2G7gxZvbzWcSDB9"
   ```

2. Create data string:
   ```
   data = "order_1MHjR7rGRo8Ykw|pay_ABC123XYZ"
   ```

3. Compute HMAC-SHA256:
   ```javascript
   // JavaScript (Node.js)
   const crypto = require('crypto');
   const data = "order_1MHjR7rGRo8Ykw|pay_ABC123XYZ";
   const secret = "Y8X46wl0J2G7gxZvbzWcSDB9";
   const signature = crypto
     .createHmac('sha256', secret)
     .update(data)
     .digest('hex');
   console.log(signature);
   ```

   ```bash
   # Using OpenSSL (macOS/Linux)
   echo -n "order_1MHjR7rGRo8Ykw|pay_ABC123XYZ" | \
     openssl dgst -sha256 -hmac "Y8X46wl0J2G7gxZvbzWcSDB9"
   ```

### Request
```bash
curl -X POST http://localhost:8085/api/v1/payments/verify \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -d '{
    "planId": "plan_basic_monthly",
    "razorpayOrderId": "order_1MHjR7rGRo8Ykw",
    "razorpayPaymentId": "pay_ABC123XYZ",
    "razorpaySignature": "9ef4dffbfd84f1318f6739a3ce19f9d85851857ae648f114332d8401e0949a3d"
  }'
```

### Payload (JSON)
```json
{
  "planId": "plan_basic_monthly",
  "razorpayOrderId": "order_1MHjR7rGRo8Ykw",
  "razorpayPaymentId": "pay_ABC123XYZ",
  "razorpaySignature": "9ef4dffbfd84f1318f6739a3ce19f9d85851857ae648f114332d8401e0949a3d"
}
```

### Expected Response
```
Payment verified successfully
```

### What Happens Behind the Scenes:
1. ✓ Verifies signature: `HMAC-SHA256(orderId|paymentId, secret) == signature`
2. ✓ Checks for duplicate payments
3. ✓ Activates subscription
4. ✓ Updates payment status to SUCCESS

---

## Step 5: Cancel Subscription

### Request
```bash
curl -X POST http://localhost:8085/api/v1/subscription/cancel \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -d '{}'
```

### Payload (JSON)
```json
{}
```

### Expected Response
```json
{
  "status": "CANCELLED",
  "message": "Subscription cancelled successfully"
}
```

---

## Alternative Payment Plans (Sample IDs)

If you have multiple plans, here are typical plan IDs:

```json
{
  "basic_monthly": "plan_basic_monthly",      // ₹499
  "premium_monthly": "plan_premium_monthly",  // ₹999
  "basic_yearly": "plan_basic_yearly",        // ₹4999
  "premium_yearly": "plan_premium_yearly",    // ₹9999
  "pro_yearly": "plan_pro_yearly"             // ₹14999
}
```

---

## Testing Scenarios

### Scenario 1: Happy Path (Full Flow)
```
OTP Verify → Get Plans → Create Order → Verify Payment → Cancel Subscription
```

### Scenario 2: Payment Verification Only
```
OTP Verify → Get Plans → Create Order → Verify Payment (Multiple times - should handle duplicates)
```

### Scenario 3: Invalid Signature
```
OTP Verify → Get Plans → Create Order → Verify Payment (with wrong signature) → Expect 400 error
```

### Scenario 4: Invalid Plan ID
```
OTP Verify → Create Order (with non-existent planId) → Expect 404 error
```

### Scenario 5: Missing Authorization
```
Create Order (without Bearer token) → Expect 401 Unauthorized
```

---

## Postman Pre-Request Script for Signature Generation

Add this to the "Pre-request Script" tab in Postman for Step 4 (Verify Payment):

```javascript
// Pre-request Script for Verify Payment request
var orderId = pm.environment.get('orderId');
var secret = pm.environment.get('razorpay_secret');

if (!orderId) {
    throw new Error('orderId not set. Run Create Order first');
}

if (!secret) {
    throw new Error('razorpay_secret not set in environment');
}

// Generate a fake payment ID
var paymentId = 'pay_' + Math.random().toString(36).substring(2, 12);
pm.environment.set('paymentId', paymentId);

// Compute HMAC-SHA256 signature
var data = orderId + '|' + paymentId;
var signature = CryptoJS.HmacSHA256(data, secret).toString();
pm.environment.set('razorpaySignature', signature);

console.log('Generated Signature for:', data);
console.log('Signature:', signature);
```

---

## Error Response Examples

### 1. Invalid Signature
```json
{
  "timestamp": "2026-07-09T10:30:45.123Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid signature",
  "path": "/api/v1/payments/verify"
}
```

### 2. Plan Not Found
```json
{
  "timestamp": "2026-07-09T10:30:45.123Z",
  "status": 404,
  "error": "Not Found",
  "message": "Plan not found",
  "path": "/api/v1/payments/create-order"
}
```

### 3. Unauthorized (Missing Token)
```json
{
  "timestamp": "2026-07-09T10:30:45.123Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource",
  "path": "/api/v1/payments/create-order"
}
```

### 4. Invalid OTP
```json
{
  "timestamp": "2026-07-09T10:30:45.123Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid OTP",
  "path": "/api/v1/auth/otp/verify"
}
```

---

## Quick Test Sequence (Copy-Paste Ready)

### Using Postman Collection
1. Import: `server/postman/SubscriptionPayment.postman_collection.json`
2. Import Environment: `server/postman/SubscriptionPayment.postman_environment.json`
3. Select Environment: "DietApp Local"
4. Run Collection in order

### Using Newman CLI
```bash
cd /Users/tarunsharma/IdeaProjects/svaadanusaar_app_dev/server

# Install newman if not installed
npm install -g newman

# Run the collection
newman run postman/SubscriptionPayment.postman_collection.json \
  -e postman/SubscriptionPayment.postman_environment.json \
  --delay-request 500 \
  -r cli,json
```

---

## Database Verification

After running the complete flow, verify in your database:

```sql
-- Check Payment Record
SELECT * FROM payments 
WHERE user_id = '550e8400-e29b-41d4-a716-446655440000';

-- Expected Output:
-- | id | user_id | order_id | payment_id | amount | status | provider |
-- | -- | --- | --- | --- | --- | --- | --- |
-- | uuid | 550e... | order_1M... | pay_ABC... | 49900 | SUCCESS | RAZORPAY |

-- Check Subscription Record
SELECT * FROM subscriptions 
WHERE user_id = '550e8400-e29b-41d4-a716-446655440000';

-- Expected Output:
-- | id | user_id | plan_id | status | start_date | end_date |
-- | -- | --- | --- | --- | --- | --- |
-- | uuid | 550e... | plan_... | ACTIVE | 2026-07-09 | 2026-08-08 |
```



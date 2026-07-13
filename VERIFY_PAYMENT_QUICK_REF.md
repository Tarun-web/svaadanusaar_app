# Testing Verify Payment API - Quick Reference

## 🎯 Quick Answer

**Q: How to test verify payment when signature & payment ID come from frontend?**

**A:** 
1. Run **create-order** endpoint first to get `orderId`
2. Generate fake `paymentId` (any format, e.g., `pay_abc123`)
3. Generate `signature` using: `HMAC-SHA256(orderId|paymentId, razorpay_secret)`
4. Call **verify** endpoint with all 4 values
5. Done! ✅

---

## 🚀 Fastest Test (3 Options)

### Option 1: Automated Script (Easiest - 1 minute)
```bash
bash test-verify-payment-complete.sh
```
✅ Generates signature automatically  
✅ Tests complete flow  
✅ Shows all values  

### Option 2: Postman (2 minutes)
```
1. Import: SubscriptionComplete.postman_collection.json
2. Run requests 1-4
3. Request 4 "Verify Payment" auto-generates signature
4. Done!
```

### Option 3: Manual cURL (3 minutes)
See section below

---

## 🔐 How to Generate Signature

### The Formula
```
signature = HMAC-SHA256(orderId|paymentId, razorpay_secret)
```

### Step by Step

**Step 1: Get orderId**
```bash
# From create-order response
orderId="order_1MHjR7rGRo8Ykw"
```

**Step 2: Generate paymentId**
```bash
# Any format works for testing
paymentId="pay_$(openssl rand -hex 6)"
# Example: pay_a1b2c3d4e5f6
```

**Step 3: Create data string**
```bash
data="${orderId}|${paymentId}"
# Example: order_1MHjR7rGRo8Ykw|pay_a1b2c3d4e5f6
```

**Step 4: Generate signature**

**Using OpenSSL:**
```bash
signature=$(echo -n "$data" | \
  openssl dgst -sha256 -hmac "Y8X46wl0J2G7gxZvbzWcSDB9" -hex | \
  sed 's/^.* //')
echo $signature
```

**Using Node.js:**
```javascript
const crypto = require('crypto');
const data = 'order_1MHjR7rGRo8Ykw|pay_a1b2c3d4e5f6';
const signature = crypto
  .createHmac('sha256', 'Y8X46wl0J2G7gxZvbzWcSDB9')
  .update(data)
  .digest('hex');
console.log(signature);
```

---

## 📝 Complete Manual cURL Test

```bash
# 1. OTP Verify
TOKEN=$(curl -s -X POST http://localhost:8085/api/v1/auth/otp/verify \
  -H "Content-Type: application/json" \
  -d '{"phone":"+911234567890","otp":"123456"}' | jq -r '.token')

# 2. Get Plans
PLAN=$(curl -s http://localhost:8085/api/v1/subscription/plans/active | jq -r '.[0].id')

# 3. Create Order
ORDER=$(curl -s -X POST http://localhost:8085/api/v1/payments/create-order \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{\"planId\":\"$PLAN\"}" | jq -r '.orderId')

# 4. Generate signature
PAYMENT="pay_$(openssl rand -hex 6)"
DATA="${ORDER}|${PAYMENT}"
SIGNATURE=$(echo -n "$DATA" | openssl dgst -sha256 -hmac "Y8X46wl0J2G7gxZvbzWcSDB9" -hex | sed 's/^.* //')

# 5. Verify Payment
curl -X POST http://localhost:8085/api/v1/payments/verify \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{
    \"planId\":\"$PLAN\",
    \"razorpayOrderId\":\"$ORDER\",
    \"razorpayPaymentId\":\"$PAYMENT\",
    \"razorpaySignature\":\"$SIGNATURE\"
  }" | jq .
```

---

## ✅ Expected Results

### Success (200)
```json
{
  "message": "Payment verified successfully"
}
```
✅ Status: 200  
✅ Subscription created (ACTIVE)  
✅ Payment record created (SUCCESS)  

### Error: Invalid Signature (400)
```json
{
  "error": "Invalid signature"
}
```
❌ Check signature generation  
❌ Verify orderId and paymentId match  

### Error: Unauthorized (401)
```json
{
  "error": "Unauthorized"
}
```
❌ Missing or invalid Bearer token  

---

## 🧪 Testing Scenarios

| Scenario | Input | Expected |
|----------|-------|----------|
| **Valid** | Correct signature | 200 ✅ |
| **Invalid Sig** | Wrong signature | 400 ❌ |
| **Duplicate** | Same paymentId twice | 200 ✅ (prevents duplicate) |
| **Invalid Plan** | Non-existent planId | 400 ❌ |
| **No Auth** | Missing token | 401 ❌ |

---

## 📊 Database Verification

After successful verify:

```sql
-- Check Payment
SELECT * FROM payments WHERE order_id = 'order_1MHjR7rGRo8Ykw';
-- Expected: status = SUCCESS, payment_id = pay_xxx

-- Check Subscription
SELECT * FROM user_subscriptions WHERE user_id = 'YOUR_USER_ID';
-- Expected: status = ACTIVE, ends_at = ~30 days from now
```

---

## 🎯 Key Points

1. **orderId** — From create-order response
2. **paymentId** — Generate fake: `pay_` + random (any format)
3. **signature** — HMAC-SHA256 of `orderId|paymentId` with secret
4. **Razorpay Secret** — In `application.properties`: `Y8X46wl0J2G7gxZvbzWcSDB9`
5. **Format** — `orderId|paymentId` (pipe-separated, NO spaces)

---

## 🔧 Troubleshooting

| Problem | Solution |
|---------|----------|
| "Invalid signature" | Check data format (orderId\|paymentId), use correct secret |
| "Unauthorized" | Add Bearer token in Authorization header |
| "Plan not found" | Use valid planId from get-plans endpoint |
| No response | Check BASE_URL is correct (http://localhost:8085) |

---

## 💡 Frontend Implementation

```javascript
// After Razorpay payment succeeds
const paymentDetails = {
  razorpay_order_id,    // From Razorpay
  razorpay_payment_id,  // From Razorpay
  razorpay_signature    // From Razorpay
};

// Send to backend
fetch('/api/v1/payments/verify', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${authToken}`
  },
  body: JSON.stringify({
    planId: selectedPlanId,
    razorpayOrderId: paymentDetails.razorpay_order_id,
    razorpayPaymentId: paymentDetails.razorpay_payment_id,
    razorpaySignature: paymentDetails.razorpay_signature
  })
}).then(r => r.json()).then(data => {
  if (data.message) {
    console.log('✅ Payment verified!');
  }
});
```

---

## 📚 Files Reference

| File | Purpose |
|------|---------|
| `VERIFY_PAYMENT_TESTING_GUIDE.md` | Complete testing guide |
| `test-verify-payment-complete.sh` | Automated test script |
| `SubscriptionComplete.postman_collection.json` | Postman collection |

---

## ⚡ One-Liner Tests

```bash
# Test signature generation
echo -n "order_1MHjR7rGRo8Ykw|pay_abc123" | openssl dgst -sha256 -hmac "Y8X46wl0J2G7gxZvbzWcSDB9" -hex

# Run full test
bash test-verify-payment-complete.sh
```

---

**Status: ✅ Ready to Test Verify Payment API!**



# ✅ Testing Verify Payment API - Summary

**Your Question:** How to test verify payment API when signature & payment ID come from frontend?

**Short Answer:** Generate fake signature yourself using the same HMAC-SHA256 logic, then test!

---

## 🎯 The 60-Second Answer

### What Happens in Production
```
Frontend ← Razorpay → Backend
Razorpay gives frontend: paymentId, orderId, signature
Frontend sends to backend verify endpoint
Backend validates signature to ensure payment is real
```

### How to Test
```
You simulate what Razorpay gives:
1. Create order (real) → get orderId
2. Generate fake paymentId
3. Generate signature: HMAC-SHA256(orderId|paymentId, secret)
4. Call verify endpoint
5. Done! ✅
```

### Why This Works
✅ Same verification logic runs in both test and production  
✅ Only difference: You generate fake signature instead of Razorpay  
✅ Tests the actual security validation that matters  

---

## 🚀 Test Verify Payment in 3 Ways

### Way 1: Automated Script (1 minute) ⭐ Best
```bash
bash test-verify-payment-complete.sh
```
✅ Generates signature automatically  
✅ Tests complete flow  
✅ Shows all values  

### Way 2: Postman (2 minutes)
```
Import: SubscriptionComplete.postman_collection.json
Run: Request 1-4
Request 4 auto-generates signature
```

### Way 3: Manual Commands (3 minutes)
```bash
# Generate signature
echo -n "orderId|paymentId" | openssl dgst -sha256 -hmac "secret" -hex

# Test verify endpoint
curl -X POST http://localhost:8085/api/v1/payments/verify ...
```

---

## 🔐 Signature Generation

### Formula
```
HMAC-SHA256(orderId|paymentId, razorpay_secret)
```

### Example
```
orderId: order_1MHjR7rGRo8Ykw
paymentId: pay_abc123xyz
secret: Y8X46wl0J2G7gxZvbzWcSDB9

data = "order_1MHjR7rGRo8Ykw|pay_abc123xyz"
signature = HMAC-SHA256(data, secret) = "9ef4dffbfd84f1318f6739a3ce19f9..."
```

### Generate Using OpenSSL
```bash
echo -n "order_1MHjR7rGRo8Ykw|pay_abc123xyz" | \
  openssl dgst -sha256 -hmac "Y8X46wl0J2G7gxZvbzWcSDB9" -hex
```

---

## 📋 Complete Test Checklist

```
Before Testing:
- [ ] Spring app running: http://localhost:8085
- [ ] Database running
- [ ] application.properties has razorpay secret

During Testing:
- [ ] OTP verify → get token
- [ ] Get plans → verify planId exists
- [ ] Create order → get orderId
- [ ] Generate paymentId (fake)
- [ ] Generate signature
- [ ] Call verify endpoint
- [ ] Get "Payment verified successfully" response

After Testing:
- [ ] Check DB: payment record (status: SUCCESS)
- [ ] Check DB: subscription record (status: ACTIVE)
- [ ] Call GET /subscription/status → ACTIVE
- [ ] Call GET /subscription/history → subscription shown
```

---

## 📚 Documentation Files

| File | Purpose | Read Time |
|------|---------|-----------|
| **VERIFY_PAYMENT_QUICK_REF.md** | Quick lookup (START HERE) | 2 min |
| **VERIFY_PAYMENT_TESTING_GUIDE.md** | Complete testing guide | 15 min |
| **VERIFY_PAYMENT_COMPLETE_FLOW.md** | Frontend-backend flow | 10 min |

---

## 🎬 Test Script (Copy-Paste Ready)

**Option 1: Run Script**
```bash
bash test-verify-payment-complete.sh
```

**Option 2: Manual Commands**
```bash
#!/bin/bash

# Step 1: Auth
TOKEN=$(curl -s http://localhost:8085/api/v1/auth/otp/verify \
  -H "Content-Type: application/json" \
  -d '{"phone":"+911234567890","otp":"123456"}' | jq -r '.token')

# Step 2: Plans
PLAN=$(curl -s http://localhost:8085/api/v1/subscription/plans/active | jq -r '.[0].id')

# Step 3: Order
ORDER=$(curl -s -X POST http://localhost:8085/api/v1/payments/create-order \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{\"planId\":\"$PLAN\"}" | jq -r '.orderId')

# Step 4: Generate Signature
PAYMENT="pay_$(openssl rand -hex 6)"
SIGNATURE=$(echo -n "${ORDER}|${PAYMENT}" | \
  openssl dgst -sha256 -hmac "Y8X46wl0J2G7gxZvbzWcSDB9" -hex | \
  sed 's/^.* //')

# Step 5: Verify
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

## ✨ Key Points

1. **Generate Fake Data** — paymentId can be anything (pay_abc123)
2. **Generate Signature** — HMAC-SHA256(orderId|paymentId, secret)
3. **Call Verify** — POST with all 4 values
4. **Same Validation** — Backend uses same logic as production
5. **Tests Real Security** — Signature validation is what protects in production

---

## 🧪 Expected Responses

### Success (200) ✅
```
"Payment verified successfully"

Database:
- Payment: status = SUCCESS
- Subscription: status = ACTIVE
```

### Error: Invalid Signature (400) ❌
```
"Invalid signature"

Solution: Verify signature generation, check data format
```

### Error: Invalid Plan (400) ❌
```
"Plan not found"

Solution: Use valid planId from get-plans endpoint
```

### Error: Unauthorized (401) ❌
```
"Full authentication required"

Solution: Include Bearer token in Authorization header
```

---

## 🔍 Debugging

**If signature generation fails:**
1. Check data format: `orderId|paymentId` (pipe-separated, no spaces)
2. Check secret: `Y8X46wl0J2G7gxZvbzWcSDB9` (from application.properties)
3. Verify algorithm: HMAC-SHA256 (not MD5 or SHA1)
4. Output should be 64 character hex string

**If verify endpoint fails:**
1. Check auth token is valid (from OTP verify)
2. Check planId exists (from get-plans)
3. Check orderId matches create-order response
4. Check signature is 64 character hex string
5. Check signature format (lowercase, no spaces)

---

## 🎯 Next Steps

1. **Immediate:** Run `bash test-verify-payment-complete.sh`
2. **Verify:** Check database records created
3. **Understand:** Read `VERIFY_PAYMENT_COMPLETE_FLOW.md`
4. **Integrate:** Use examples from docs to build frontend

---

## 📞 Quick Reference

| Need | Command |
|------|---------|
| Run full test | `bash test-verify-payment-complete.sh` |
| Generate signature | `echo -n "data" \| openssl dgst -sha256 -hmac "secret" -hex` |
| Generate paymentId | `openssl rand -hex 6` (prepend "pay_") |
| Check response | `curl ... \| jq .` |
| View database | See SQL queries in testing guide |

---

## ✅ Final Checklist

- [ ] Read this summary
- [ ] Run test script: `bash test-verify-payment-complete.sh`
- [ ] Verify response is "Payment verified successfully"
- [ ] Check database for Payment and Subscription records
- [ ] Ready to integrate with frontend!

---

**Status: ✅ Ready to Test Verify Payment!**

Start with: `bash test-verify-payment-complete.sh`

Questions? See: `VERIFY_PAYMENT_COMPLETE_FLOW.md`



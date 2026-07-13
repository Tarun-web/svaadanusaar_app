# Verify Payment API - Visual Testing Guide

## 🎬 The Complete Picture

### Production Flow (Real Razorpay)
```
┌──────────────────────────────────────────────────────────────────────┐
│                         PRODUCTION FLOW                              │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  User's Browser (Frontend)         Razorpay                Backend  │
│  ───────────────────────────────────────────────────────────────   │
│                                                                      │
│  ① Click "Subscribe"                                               │
│     │                                                              │
│     ├─→ Open Razorpay Payment Form ────→ [Razorpay UI]            │
│     │                                                              │
│     │  ② User enters card details                                  │
│     │                                                              │
│     └─→ Process Payment ────────────→ ✅ Payment Success          │
│                                       │                           │
│         Razorpay Returns:             │                           │
│         • paymentId                   │                           │
│         • orderId                     │                           │
│         • signature (HMAC-SHA256)     │                           │
│                                       │                           │
│  ③ POST /payments/verify ────────────────→ Verify Endpoint       │
│     with: orderId, paymentId, signature   │                       │
│                                           ↓                       │
│         ← 200 Success ◄─────────────── ✅ Signature Valid        │
│                                         ✅ Create Subscription   │
│  ④ Show Success                                                   │
│     "Subscription Active!" ✅                                      │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

---

### Testing Flow (Simulated Razorpay)
```
┌──────────────────────────────────────────────────────────────────────┐
│                         TESTING FLOW                                 │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  Your Terminal (Simulating Frontend)     Backend                    │
│  ──────────────────────────────────────────────────────────────     │
│                                                                      │
│  ① curl /payments/create-order ──→ Get orderId                     │
│     orderId = "order_1MHjR7rGRo8Ykw"                               │
│                                                                      │
│  ② Generate Fake Razorpay Response:                                │
│     paymentId = "pay_abc123xyz"                                    │
│     (any format, we make it up)                                    │
│                                                                      │
│  ③ Generate Signature (Simulating Razorpay):                       │
│     data = orderId|paymentId                                       │
│     signature = HMAC-SHA256(data, secret)                          │
│                                                                      │
│  ④ curl /payments/verify ─────────→ Verify Endpoint              │
│     with: orderId, paymentId, signature │                         │
│     ┌─────────────────────────────────┐ │                         │
│     │ Backend checks:                 │ │                         │
│     │ HMAC-SHA256 match? ✅           │ │                         │
│     │ YES!                            │ │                         │
│     │ → Create subscription           │ │                         │
│     └─────────────────────────────────┘ │                         │
│            ← 200 Success ◄───────────────                          │
│     "Payment verified successfully"                                 │
│                                                                      │
│  ⑤ ✅ Test Complete                                                 │
│     Check Database:                                                 │
│     • Payment record (SUCCESS)                                      │
│     • Subscription record (ACTIVE)                                  │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

---

## 🔐 Signature Generation (Visual)

```
┌──────────────────────────────────────────────────────┐
│            SIGNATURE GENERATION                      │
├──────────────────────────────────────────────────────┤
│                                                      │
│  Input 1: Order ID                                  │
│  ┌────────────────────────────────────────────┐    │
│  │ order_1MHjR7rGRo8Ykw                       │    │
│  └────────────────────────────────────────────┘    │
│                                                      │
│  Input 2: Payment ID (Generated)                    │
│  ┌────────────────────────────────────────────┐    │
│  │ pay_a1b2c3d4e5f6                           │    │
│  └────────────────────────────────────────────┘    │
│                                                      │
│  ⬇️  Combine with pipe (|)                           │
│  ┌────────────────────────────────────────────┐    │
│  │ order_1MHjR7rGRo8Ykw|pay_a1b2c3d4e5f6    │    │
│  └────────────────────────────────────────────┘    │
│                                                      │
│  Input 3: Secret (from application.properties)     │
│  ┌────────────────────────────────────────────┐    │
│  │ Y8X46wl0J2G7gxZvbzWcSDB9                   │    │
│  └────────────────────────────────────────────┘    │
│                                                      │
│  ⬇️  Algorithm: HMAC-SHA256                         │
│  ┌────────────────────────────────────────────┐    │
│  │ HMAC-SHA256(combined_string, secret)       │    │
│  └────────────────────────────────────────────┘    │
│                                                      │
│  Output: Signature (64 character hex)              │
│  ┌────────────────────────────────────────────┐    │
│  │ 9ef4dffbfd84f1318f6739a3ce19f9d85851857   │    │
│  │ ae648f114332d8401e0949a3d                  │    │
│  └────────────────────────────────────────────┘    │
│                                                      │
└──────────────────────────────────────────────────────┘
```

---

## 📊 API Call Diagram

```
┌─────────────────────────────────────────────────┐
│   POST /api/v1/payments/verify                  │
├─────────────────────────────────────────────────┤
│                                                 │
│  Headers:                                       │
│  ┌──────────────────────────────────────────┐  │
│  │ Content-Type: application/json           │  │
│  │ Authorization: Bearer {token}            │  │
│  └──────────────────────────────────────────┘  │
│                                                 │
│  Body (JSON):                                   │
│  ┌──────────────────────────────────────────┐  │
│  │ {                                        │  │
│  │   "planId": "plan_basic_monthly",        │  │
│  │   "razorpayOrderId": "order_xxx",        │  │
│  │   "razorpayPaymentId": "pay_yyy",        │  │
│  │   "razorpaySignature": "zzz..."          │  │
│  │ }                                        │  │
│  └──────────────────────────────────────────┘  │
│                                                 │
│              ⬇️ Backend Processing               │
│                                                 │
│  ┌──────────────────────────────────────────┐  │
│  │ 1. Validate token ✅                     │  │
│  │ 2. Get order from DB ✅                  │  │
│  │ 3. Regenerate signature ✅               │  │
│  │ 4. Compare signatures ✅                 │  │
│  │ 5. Create subscription ✅                │  │
│  └──────────────────────────────────────────┘  │
│                                                 │
│  Response:                                      │
│  ┌──────────────────────────────────────────┐  │
│  │ Status: 200                              │  │
│  │ Body: "Payment verified successfully"    │  │
│  └──────────────────────────────────────────┘  │
│                                                 │
└─────────────────────────────────────────────────┘
```

---

## 🧪 Testing Workflow (Step by Step)

```
START
  │
  ├─→ 1️⃣  OTP Verify
  │      curl http://localhost:8085/api/v1/auth/otp/verify
  │      GET: token
  │
  ├─→ 2️⃣  Get Plans
  │      curl http://localhost:8085/api/v1/subscription/plans/active
  │      GET: planId
  │
  ├─→ 3️⃣  Create Order
  │      curl POST /payments/create-order
  │      GET: orderId
  │
  ├─→ 4️⃣  Generate Fake Payment Data
  │      paymentId = "pay_" + random()
  │      signature = HMAC-SHA256(orderId|paymentId, secret)
  │
  ├─→ 5️⃣  Verify Payment ⭐ MAIN TEST
  │      curl POST /payments/verify
  │      with: orderId, paymentId, signature
  │      GET: "Payment verified successfully" ✅
  │
  ├─→ 6️⃣  Verify Results
  │      Check Database:
  │      • payments table (status: SUCCESS)
  │      • user_subscriptions table (status: ACTIVE)
  │
  ✅ END (Test Passed!)
```

---

## 📋 Test Data Example

```
┌────────────────────────────────────────────────────┐
│  ACTUAL VALUES YOU'LL GET                          │
├────────────────────────────────────────────────────┤
│                                                    │
│  From OTP Verify:                                  │
│  token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." │
│  userId = "550e8400-e29b-41d4-a716-446655440000" │
│                                                    │
│  From Get Plans:                                   │
│  planId = "plan_basic_monthly"                     │
│                                                    │
│  From Create Order:                                │
│  orderId = "order_1MHjR7rGRo8Ykw"                  │
│                                                    │
│  You Generate:                                     │
│  paymentId = "pay_a1b2c3d4e5f6"                   │
│  data = "order_1MHjR7rGRo8Ykw|pay_a1b2c3d4e5f6" │
│  signature = "9ef4dffbfd84f1318f6739a3ce19f9..." │
│                                                    │
│  Call Verify With All 4:                           │
│  planId = "plan_basic_monthly"                     │
│  orderId = "order_1MHjR7rGRo8Ykw"                  │
│  paymentId = "pay_a1b2c3d4e5f6"                   │
│  signature = "9ef4dffbfd84f1318f6739a3ce19f9..." │
│                                                    │
└────────────────────────────────────────────────────┘
```

---

## ✅ Success Indicators

```
✅ If Signature is VALID:
┌─────────────────────────────┐
│ Response: 200               │
│ Body: "Payment verified     │
│       successfully"         │
│                             │
│ Database Changes:           │
│ • Payment: SUCCESS ✅       │
│ • Subscription: ACTIVE ✅   │
│                             │
│ Next API Calls Work:        │
│ • GET /subscription/status  │
│  → Returns: ACTIVE ✅       │
│ • GET /subscription/history │
│  → Shows subscription ✅    │
└─────────────────────────────┘

❌ If Signature is INVALID:
┌─────────────────────────────┐
│ Response: 400               │
│ Body: "Invalid signature"   │
│                             │
│ Database Changes:           │
│ • NONE (Payment rejected)   │
│                             │
│ Fix: Regenerate signature   │
│ or check:                   │
│ • orderId correct?          │
│ • paymentId correct?        │
│ • Secret correct?           │
│ • Format: orderId|paymentId?│
└─────────────────────────────┘
```

---

## 🎯 Quick Start

```
ONE COMMAND TO TEST:

bash test-verify-payment-complete.sh

This will:
1. ✅ OTP Verify
2. ✅ Get Plans
3. ✅ Create Order
4. ✅ Generate Fake Payment Data
5. ✅ Generate Signature Automatically
6. ✅ Call Verify Endpoint
7. ✅ Show Results
8. ✅ Print Database Query Commands

Total Time: ~2 minutes
```

---

## 📚 Documentation Map

```
Your Question ──→ "How to test verify payment when signature comes from frontend?"
    │
    ├─→ VERIFY_PAYMENT_SUMMARY.md ◄─── START HERE (This)
    │   └─→ 60-second answer + checklist
    │
    ├─→ VERIFY_PAYMENT_QUICK_REF.md
    │   └─→ Quick copy-paste commands
    │
    ├─→ VERIFY_PAYMENT_TESTING_GUIDE.md
    │   └─→ Detailed testing methods + scenarios
    │
    └─→ VERIFY_PAYMENT_COMPLETE_FLOW.md
        └─→ Full frontend-backend integration flow
```

---

## 🏃 Action Items

1. **Now:** `bash test-verify-payment-complete.sh`
2. **Next:** Check database for Payment and Subscription records
3. **Then:** Read `VERIFY_PAYMENT_COMPLETE_FLOW.md` to understand flow
4. **Finally:** Implement frontend integration using example code

---

**Status: ✅ Ready to Test!**

Run: `bash test-verify-payment-complete.sh`



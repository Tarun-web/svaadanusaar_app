# Subscription Features - Step-by-Step Testing Guide

## Quick Start (5 mins)

### Prerequisites
- Spring Boot app running on `http://localhost:8085`
- Database running with migrations applied
- Postman installed (or use cURL)

### Test in 3 Steps

1. **Import & Run Postman Collection**
   ```
   File → Import → Select: server/postman/SubscriptionComplete.postman_collection.json
   ```

2. **Set Environment Variables**
   - Import: `server/postman/SubscriptionPayment.postman_environment.json`
   - Select: "DietApp Local"

3. **Run Requests**
   - Click "Run" → select all 7 requests → "Run Collection"
   - Watch as each request executes and auto-saves variables

---

## Detailed Testing (Step-by-Step)

### Step 1: Authenticate User

**Endpoint:** `POST /api/v1/auth/otp/verify`

**cURL:**
```bash
curl -X POST http://localhost:8085/api/v1/auth/otp/verify \
  -H "Content-Type: application/json" \
  -d '{
    "phone": "+911234567890",
    "otp": "123456"
  }'
```

**Expected Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userId": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Save:** `token` and `userId` for next requests

---

### Step 2: Get Available Plans

**Endpoint:** `GET /api/v1/subscription/plans/active`

**cURL:**
```bash
curl -X GET http://localhost:8085/api/v1/subscription/plans/active
```

**Expected Response:**
```json
[
  {
    "id": "plan_basic_monthly",
    "months": 1,
    "price": 499,
    "chatbotDailyLimit": 50
  },
  {
    "id": "plan_premium_yearly",
    "months": 12,
    "price": 4999,
    "chatbotDailyLimit": 500
  }
]
```

**Save:** `planId` (e.g., "plan_basic_monthly")

---

### Step 3: Create Order

**Endpoint:** `POST /api/v1/payments/create-order`

**cURL:**
```bash
curl -X POST http://localhost:8085/api/v1/payments/create-order \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN_FROM_STEP_1" \
  -d '{
    "planId": "plan_basic_monthly"
  }'
```

**Expected Response:**
```json
{
  "orderId": "order_1MHjR7rGRo8Ykw",
  "amount": 49900,
  "currency": "INR",
  "keyId": "rzp_live_1a2b3c4d5e6f7g8h"
}
```

**Save:** `orderId`

---

### Step 4: Verify Payment

**Endpoint:** `POST /api/v1/payments/verify`

**Steps:**
1. Generate fake payment ID: `pay_` + random hex
2. Create signature: `HMAC-SHA256(orderId|paymentId, razorpay_secret)`

**Signature Calculation:**
```bash
# Manual calculation
orderId="order_1MHjR7rGRo8Ykw"
paymentId="pay_abc123xyz"
secret="Y8X46wl0J2G7gxZvbzWcSDB9"

data="${orderId}|${paymentId}"
signature=$(echo -n "$data" | openssl dgst -sha256 -hmac "$secret" -hex | sed 's/^.* //')
echo $signature
```

**cURL:**
```bash
curl -X POST http://localhost:8085/api/v1/payments/verify \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "planId": "plan_basic_monthly",
    "razorpayOrderId": "order_1MHjR7rGRo8Ykw",
    "razorpayPaymentId": "pay_abc123xyz",
    "razorpaySignature": "9ef4dffbfd84f1318f6739a3ce19f9d85851857ae648f114332d8401e0949a3d"
  }'
```

**Expected Response:**
```
Payment verified successfully
```

✅ **At this point: Subscription is ACTIVE!**

---

## 🆕 NEW FEATURES TESTING

### Step 5: Get Subscription Status

**Endpoint:** `GET /api/v1/subscription/status` ← **NEW**

**Purpose:** Check if user has active subscription

**cURL:**
```bash
curl -X GET http://localhost:8085/api/v1/subscription/status \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Expected Response (ACTIVE):**
```json
{
  "planId": "plan_basic_monthly",
  "startsAt": "2026-07-10T08:00:00Z",
  "endsAt": "2026-08-10T08:00:00Z",
  "status": "ACTIVE"
}
```

**Verify:**
- ✅ Status = "ACTIVE"
- ✅ endsAt is about 30 days from now
- ✅ Returns 200

---

### Step 6: Get Subscription History

**Endpoint:** `GET /api/v1/subscription/history` ← **NEW**

**Purpose:** View all past and current subscriptions

**cURL:**
```bash
curl -X GET http://localhost:8085/api/v1/subscription/history \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Expected Response:**
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440001",
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "planId": "plan_basic_monthly",
    "startsAt": "2026-07-10T08:00:00Z",
    "endsAt": "2026-08-10T08:00:00Z",
    "status": "ACTIVE",
    "autoRenew": false,
    "createdAt": "2026-07-10T08:00:00Z",
    "updatedAt": "2026-07-10T08:00:00Z"
  }
]
```

**Verify:**
- ✅ Returns array with 1 subscription
- ✅ Status = "ACTIVE"
- ✅ Ordered by newest first

---

### Step 7: Cancel Subscription

**Endpoint:** `POST /api/v1/subscription/cancel`

**cURL:**
```bash
curl -X POST http://localhost:8085/api/v1/subscription/cancel \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{}'
```

**Expected Response:**
```json
{
  "planId": "plan_basic_monthly",
  "startsAt": "2026-07-10T08:00:00Z",
  "endsAt": "2026-07-10T12:30:45Z",
  "status": "CANCELLED"
}
```

**Verify:**
- ✅ Status = "CANCELLED"
- ✅ endsAt = now (cancellation time)
- ✅ Returns 200

---

### Step 8: Verify Status Changed (Final Check)

**Endpoint:** `GET /api/v1/subscription/status`

**cURL:**
```bash
curl -X GET http://localhost:8085/api/v1/subscription/status \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Expected Response:**
```json
{
  "planId": null,
  "startsAt": null,
  "endsAt": null,
  "status": "NO_SUBSCRIPTION"
}
```

**Verify:**
- ✅ Status = "NO_SUBSCRIPTION" (or "CANCELLED" depending on logic)
- ✅ User is no longer subscribed

✅ **Complete test flow passed!**

---

## Testing Scenarios

### Scenario A: Complete Lifecycle (Full Test)
```
Auth → Plans → Create Order → Verify Payment → 
Get Status (ACTIVE) → Get History (1 sub) → Cancel → Get Status (NO_SUBSCRIPTION)
```
**Expected:** All 200s, correct status transitions

### Scenario B: Status Without Payment (No Subscription)
```
Auth → Get Status
```
**Expected:** Status = "NO_SUBSCRIPTION"

### Scenario C: History Without Subscription
```
Auth → Get History
```
**Expected:** Empty array `[]`

### Scenario D: Expired Subscription (Manual DB Test)
```
1. Create subscription with endsAt = yesterday (in DB)
2. GET /subscription/status
3. Check status
```
**Expected:** Auto-marked as "EXPIRED"

---

## Scheduler Testing

### Test: Verify Daily Job Runs

**Configuration:**
- Default: Runs at **02:00 AM UTC** every day
- For testing: Uncomment 5-minute cron in `SubscriptionScheduler.java`

**Testing Steps:**

1. **Check logs at 02:00 UTC:**
   ```
   Look for in application console:
   INFO: Starting scheduled task: markExpiredSubscriptions
   INFO: Completed scheduled task: markExpiredSubscriptions
   ```

2. **For immediate testing (dev only):**
   - Edit `SubscriptionScheduler.java`
   - Uncomment the 5-minute scheduler (lines 32-38)
   - Restart app
   - Wait 5 minutes → check logs

3. **Manual trigger (if needed):**
   ```java
   // In a controller or test
   @Autowired
   private SubscriptionService subscriptionService;
   
   subscriptionService.markExpiredSubscriptions(); // Manually call
   ```

---

## Error Handling & Troubleshooting

### ❌ 401 Unauthorized
**Cause:** Missing or invalid Bearer token
**Solution:** Run Step 1 first, copy token to Authorization header

```bash
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### ❌ 404 Not Found on /subscription/status
**Cause:** Endpoint not deployed (code changes not picked up)
**Solution:** 
1. Stop Spring Boot app
2. Run: `mvn clean compile`
3. Restart app
4. Try again

### ❌ GET /status returns NO_SUBSCRIPTION when should be ACTIVE
**Cause:** No active subscription created yet
**Solution:** Complete the payment flow first (Steps 1–4)

### ❌ Scheduler not running at 02:00 AM
**Cause:** `@EnableScheduling` not in main app class
**Solution:** Verify `DietAppApplication.java` has `@EnableScheduling`

```java
@SpringBootApplication
@EnableScheduling  // ← This must be present
public class DietAppApplication { ... }
```

### ❌ Compilation errors in IDE
**Solution:**
```bash
cd /Users/tarunsharma/IdeaProjects/svaadanusaar_app_dev/server
mvn clean compile
```

---

## Database Verification (SQL)

### Check Payment Record
```sql
SELECT * FROM payments 
WHERE user_id = '550e8400-e29b-41d4-a716-446655440000'
ORDER BY created_at DESC;
```

**Expected:**
- `status` = "SUCCESS"
- `payment_id` = generated payment ID
- `amount` = 49900 (for ₹499)

### Check Subscription Record
```sql
SELECT * FROM user_subscriptions 
WHERE user_id = '550e8400-e29b-41d4-a716-446655440000'
ORDER BY created_at DESC;
```

**Expected (after payment):**
- `status` = "ACTIVE"
- `starts_at` = now
- `ends_at` = 30 days from now

**Expected (after cancel):**
- `status` = "CANCELLED"

### List All Expired Subscriptions (for scheduler testing)
```sql
SELECT * FROM user_subscriptions 
WHERE ends_at < NOW() AND status != 'EXPIRED' AND status != 'CANCELLED';
```

**Expected (after scheduler runs):** All have `status` = "EXPIRED"

---

## Postman Automation

### Quick Run (Collection Mode)
1. Open Postman
2. Import: `SubscriptionComplete.postman_collection.json`
3. Click "Run" in collection
4. Watch all 7 requests execute automatically

### Advanced: Use Newman (CLI)
```bash
# Install newman (one-time)
npm install -g newman

# Run collection
cd /Users/tarunsharma/IdeaProjects/svaadanusaar_app_dev/server

newman run postman/SubscriptionComplete.postman_collection.json \
  -e postman/SubscriptionPayment.postman_environment.json \
  --delay-request 500 \
  -r cli,json

# Outputs test results and JSON report
```

---

## Final Checklist

Before calling this complete:

- [ ] Code compiles: `mvn clean compile` ✅
- [ ] All 5 Java files have no errors ✅
- [ ] Postman collection imports successfully ✅
- [ ] GET /status returns 200 with correct status ✅
- [ ] GET /history returns 200 with subscription array ✅
- [ ] Scheduler is enabled (@EnableScheduling present) ✅
- [ ] Database shows correct subscription records ✅
- [ ] Payment flow (Steps 1–4) still works ✅
- [ ] Status transitions: ACTIVE → CANCELLED ✅

---

## What's Next?

✅ **Subscriptions are complete!** Move to:
1. **Health Profile** (independent feature)
2. **Chatbot** (can gate by subscription status)
3. **Notifications** (expiry reminders)
4. **Renewal Logic** (auto-renew feature)

---

## Support

**Reference Files:**
- API Docs: `SUBSCRIPTION_FEATURES_COMPLETE.md`
- Build Summary: `SUBSCRIPTION_BUILD_SUMMARY.md`
- Test Script: `test-subscription-features.sh`
- Postman Collection: `server/postman/SubscriptionComplete.postman_collection.json`



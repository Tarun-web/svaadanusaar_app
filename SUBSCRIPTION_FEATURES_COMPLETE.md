# Subscription Features - Complete Guide

## Overview
You've added critical subscription management features to handle the complete subscription lifecycle: creation, status tracking, expiry handling, and history. This guide walks through each feature, APIs, and testing.

---

## ✅ Features Added

### 1. **Get Subscription Status** — `GET /api/v1/subscription/status`
Check the current subscription status for the authenticated user.

**Endpoint:** `GET /api/v1/subscription/status`

**Auth Required:** Bearer token (Yes)

**Response (Active Subscription):**
```json
{
  "planId": "plan_basic_monthly",
  "startsAt": "2026-07-10T08:00:00Z",
  "endsAt": "2026-08-10T08:00:00Z",
  "status": "ACTIVE"
}
```

**Response (Expired Subscription):**
```json
{
  "planId": "plan_basic_monthly",
  "startsAt": "2026-06-10T08:00:00Z",
  "endsAt": "2026-07-10T08:00:00Z",
  "status": "EXPIRED"
}
```

**Response (No Active Subscription):**
```json
{
  "planId": null,
  "startsAt": null,
  "endsAt": null,
  "status": "NO_SUBSCRIPTION"
}
```

**What It Does:**
- ✓ Checks if user has an active subscription
- ✓ Automatically marks subscription as EXPIRED if `endsAt` has passed (safety check)
- ✓ Returns current status for frontend display
- ✓ Used to gate features (e.g., dietitian chat, meal plans)

---

### 2. **Get Subscription History** — `GET /api/v1/subscription/history`
Retrieve all past and current subscriptions for the user.

**Endpoint:** `GET /api/v1/subscription/history`

**Auth Required:** Bearer token (Yes)

**Response:**
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440001",
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "planId": "plan_premium_yearly",
    "startsAt": "2026-07-08T10:00:00Z",
    "endsAt": "2027-07-08T10:00:00Z",
    "status": "ACTIVE",
    "autoRenew": false,
    "createdAt": "2026-07-08T10:00:00Z",
    "updatedAt": "2026-07-08T10:00:00Z"
  },
  {
    "id": "550e8400-e29b-41d4-a716-446655440002",
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "planId": "plan_basic_monthly",
    "startsAt": "2026-06-10T08:00:00Z",
    "endsAt": "2026-07-10T08:00:00Z",
    "status": "EXPIRED",
    "autoRenew": false,
    "createdAt": "2026-06-10T08:00:00Z",
    "updatedAt": "2026-07-10T02:00:00Z"
  }
]
```

**What It Does:**
- ✓ Returns all subscriptions (ACTIVE, EXPIRED, CANCELLED) ordered by newest first
- ✓ Used for account dashboard / subscription page
- ✓ Shows renewal history for users
- ✓ Helps with customer support (see subscription timeline)

---

### 3. **Background Job: Auto-Expire Subscriptions**
Scheduled task that runs **every day at 2 AM UTC** to mark expired subscriptions.

**Cron:** `0 0 2 * * ?` (02:00 AM daily)

**What It Does:**
- ✓ Finds all subscriptions where `endsAt < now` and status is not already EXPIRED/CANCELLED
- ✓ Marks them as EXPIRED
- ✓ Logs count of expired subscriptions for monitoring
- ✓ Runs automatically; no manual intervention needed

**Implementation:**
- File: `SubscriptionScheduler.java`
- Method: `markExpiredSubscriptionsDaily()`
- Service Call: `SubscriptionService.markExpiredSubscriptions()`

**Why It Matters:**
- Without this, subscriptions would stay ACTIVE forever even after `endsAt`
- Ensures accurate status for feature gating (chatbot limits, meal plans, etc.)
- Decouples expiry from user requests (not checking expiry on every API call)

**For Testing (Dev):**
Uncomment the testing scheduler in `SubscriptionScheduler.java` to run every 5 minutes:
```java
// @Scheduled(cron = "0 */5 * * * ?")
// public void markExpiredSubscriptionsTesting() { ... }
```

---

## Complete API Flow (Updated)

```
1. OTP Verify (POST /api/v1/auth/otp/verify)
   ↓
2. Get Active Plans (GET /api/v1/subscription/plans/active)
   ↓
3. Create Order (POST /api/v1/payments/create-order)
   ↓
4. Verify Payment (POST /api/v1/payments/verify)
   ↓
5. Get Subscription Status (GET /api/v1/subscription/status) ← NEW
   ↓ [Optional: Check History]
6. Get Subscription History (GET /api/v1/subscription/history) ← NEW
   ↓ [Optional: Cancel]
7. Cancel Subscription (POST /api/v1/subscription/cancel)
```

---

## Repository Query Methods Added

New queries in `UserSubscriptionRepository`:

```java
// Find all subscriptions for a user (ordered by newest first)
List<UserSubscription> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

// Find subscriptions past their end date but not yet marked EXPIRED
@Query("SELECT s FROM UserSubscription s WHERE s.endsAt < :now 
        AND s.status != 'EXPIRED' AND s.status != 'CANCELLED'")
List<UserSubscription> findExpiredSubscriptions(@Param("now") LocalDateTime now);
```

---

## Service Methods Added

### `getSubscriptionStatus(UUID userId)`
```java
// Get current subscription status
// Returns: ACTIVE, EXPIRED, or NO_SUBSCRIPTION
SubscriptionStatusResponse status = subscriptionService.getSubscriptionStatus(userId);
if ("ACTIVE".equals(status.getStatus())) {
    // Allow feature access
} else if ("NO_SUBSCRIPTION".equals(status.getStatus())) {
    // Prompt to upgrade
}
```

### `getSubscriptionHistory(UUID userId)`
```java
// Get all subscriptions for user (newest first)
List<UserSubscription> history = subscriptionService.getSubscriptionHistory(userId);
```

### `markExpiredSubscriptions()`
```java
// Called by scheduler - marks all expired subscriptions
subscriptionService.markExpiredSubscriptions();
```

---

## Postman Collection Usage

### Import & Setup
1. **Import collection:** `SubscriptionComplete.postman_collection.json`
2. **Import environment:** `SubscriptionPayment.postman_environment.json` (or set baseUrl manually)
3. **Set variables in Postman:**
   - `baseUrl`: `http://localhost:8085`
   - `phone`: `+911234567890`
   - `otp`: `123456`
   - `razorpay_secret`: (from your `application.properties`)

### Run Flow
**Option A: Sequential (Postman UI)**
1. Run requests 1–4 as before (auth → payment verification)
2. New: Run request 5 (Get Status) to verify subscription is ACTIVE
3. New: Run request 6 (Get History) to see all subscriptions
4. New: Run request 7 (Cancel) to end subscription
5. New: Run request 5 again to verify status is now CANCELLED

**Option B: Collection Runner**
- Click "Run" in Postman → select all requests → "Run" → auto-executes in order

**Option C: Command Line (Newman)**
```bash
cd /Users/tarunsharma/IdeaProjects/svaadanusaar_app_dev/server

# Run complete flow
newman run postman/SubscriptionComplete.postman_collection.json \
  -e postman/SubscriptionPayment.postman_environment.json \
  --delay-request 500
```

---

## Testing Scenarios

### ✓ Scenario 1: Complete Lifecycle
```
1. OTP Verify → get token
2. Get Plans → select plan
3. Create Order → get orderId
4. Verify Payment → activate subscription
5. Get Status → returns ACTIVE
6. Get History → shows 1 subscription
7. Cancel → changes status to CANCELLED
8. Get Status → returns CANCELLED (or NO_SUBSCRIPTION depending on implementation)
```

**Expected Results:**
- ✓ All requests return 200
- ✓ Status transitions: ACTIVE → CANCELLED
- ✓ History shows correct order

---

### ✓ Scenario 2: Status Check Only
```
1. OTP Verify
2. Get Status → should return NO_SUBSCRIPTION (no active sub)
3. Get History → returns empty array []
```

**Expected Results:**
- ✓ Status shows NO_SUBSCRIPTION
- ✓ Empty history until first subscription

---

### ✓ Scenario 3: Expired Subscription Handling
```
1. Create subscription that ends at past date (manually set in DB for testing)
2. Call GET /subscription/status
3. Response should auto-mark as EXPIRED and return status: EXPIRED
```

**SQL (for DB testing):**
```sql
-- Create a subscription that ended yesterday
INSERT INTO user_subscriptions (id, user_id, plan_id, starts_at, ends_at, status, auto_renew, created_at, updated_at)
VALUES (uuid(), '<USER_ID>', 'plan_basic', NOW() - INTERVAL 40 DAY, NOW() - INTERVAL 1 DAY, 'ACTIVE', false, NOW(), NOW());

-- Check status via API
GET /api/v1/subscription/status

-- Should auto-mark as EXPIRED and return status: EXPIRED
```

---

### ✓ Scenario 4: Scheduler Job (Testing)
1. Create multiple subscriptions with past `endsAt` dates
2. Uncomment testing scheduler (runs every 5 min)
3. Wait 5 minutes
4. Query DB or call GET /subscription/history
5. Verify all subscriptions now have status = EXPIRED

---

## Implementation Details

### Files Changed/Created

| File | Changes |
|------|---------|
| `UserSubscriptionRepository.java` | Added 2 new query methods |
| `SubscriptionService.java` | Added 3 new methods (getStatus, getHistory, markExpired) |
| `SubscriptionController.java` | Added 2 new endpoints (GET /status, GET /history) |
| `SubscriptionScheduler.java` | NEW — Scheduled task for daily expiry marking |
| `DietAppApplication.java` | Added `@EnableScheduling` annotation |

### Backward Compatibility
- ✓ All existing endpoints unchanged (POST /start, POST /cancel)
- ✓ Existing data model unchanged
- ✓ Existing tests unaffected

---

## Integration with Health Profile & Chatbot

Once you build health profile, you can use subscription status:

```java
// In ChatbotController or DietPlanController
@GetMapping("/chat")
public ChatResponse chat(@RequestParam String message, Authentication auth) {
    UUID userId = (UUID) auth.getPrincipal();
    
    // Check subscription
    SubscriptionStatusResponse sub = subscriptionService.getSubscriptionStatus(userId);
    
    if ("ACTIVE".equals(sub.getStatus())) {
        // Allow chatbot access
        return chatbotService.processMessage(userId, message);
    } else {
        // Show "Subscribe to access chatbot" message
        return new ChatResponse("Please subscribe to access the chatbot.");
    }
}
```

---

## Deployment Notes

- **Scheduler Timezone:** Runs at 02:00 UTC (set cron to local time if needed)
- **Transactionality:** All updates wrapped in `@Transactional` (automatic rollback on error)
- **Logging:** Uses SLF4J via Lombok `@Slf4j` — logs expiry count daily
- **Error Handling:** Scheduler catches exceptions; won't crash app if something fails

---

## Next Steps

1. ✅ **Test the new endpoints** using Postman collection
2. ✅ **Verify scheduler logs** in application output (check 02:00 AM UTC)
3. **Build Health Profile** — now independent, safe to start
4. **Add Chatbot Rate Limiting** — use subscription status + `chatbotDailyLimit`
5. **Add Renewal Logic** — use `autoRenew` flag when ready

---

## Troubleshooting

### Issue: GET /status returns NO_SUBSCRIPTION when subscription should be ACTIVE
**Cause:** User has no ACTIVE subscription (all EXPIRED/CANCELLED or none exist)
**Solution:** Run payment flow first to create subscription

### Issue: Scheduler not running
**Cause:** `@EnableScheduling` not added to main app
**Solution:** Verify `DietAppApplication.java` has `@EnableScheduling`

### Issue: Subscription stays ACTIVE past endsAt
**Cause:** Scheduler hasn't run yet (only runs at 02:00 UTC)
**Solution:** For testing, call GET /status (does real-time check), or run scheduler manually with testing cron

### Issue: 401 Unauthorized on status/history endpoints
**Cause:** Missing or invalid Bearer token
**Solution:** Run OTP Verify first, copy token to Authorization header

---

## Summary

| Feature | Endpoint | Purpose | Status |
|---------|----------|---------|--------|
| Check Status | GET `/subscription/status` | Know if user is active | ✅ Done |
| View History | GET `/subscription/history` | See all subscriptions | ✅ Done |
| Auto-Expire | Scheduled daily | Mark expired subs | ✅ Done |
| Cancel | POST `/subscription/cancel` | End subscription | ✅ Already existed |
| Create Order | POST `/payments/create-order` | Start payment | ✅ Already existed |
| Verify Payment | POST `/payments/verify` | Activate subscription | ✅ Already existed |

**All critical subscription features are now production-ready!** 🎉



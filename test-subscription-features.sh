#!/bin/bash

# Subscription Features Testing - Ready-to-Use Commands
# This script demonstrates the complete subscription lifecycle

echo "🔧 Subscription Features Testing"
echo "=================================="

BASE_URL="http://localhost:8085"
PHONE="+911234567890"
OTP="123456"

# Step 1: OTP Verify
echo ""
echo "📌 Step 1: OTP Verify"
echo "====================="

STEP1=$(curl -s -X POST "$BASE_URL/api/v1/auth/otp/verify" \
  -H "Content-Type: application/json" \
  -d "{\"phone\": \"$PHONE\", \"otp\": \"$OTP\"}")

echo "$STEP1" | jq '.' 2>/dev/null || echo "$STEP1"

AUTH_TOKEN=$(echo "$STEP1" | jq -r '.token' 2>/dev/null)
USER_ID=$(echo "$STEP1" | jq -r '.userId' 2>/dev/null)

if [ -z "$AUTH_TOKEN" ] || [ "$AUTH_TOKEN" = "null" ]; then
  echo "❌ Failed to get auth token"
  exit 1
fi

echo "✅ Token obtained"

# Step 2: Get Plans
echo ""
echo "📌 Step 2: Get Active Plans"
echo "==========================="

STEP2=$(curl -s -X GET "$BASE_URL/api/v1/subscription/plans/active")
echo "$STEP2" | jq '.' 2>/dev/null || echo "$STEP2"

PLAN_ID=$(echo "$STEP2" | jq -r '.[0].id' 2>/dev/null)

if [ -z "$PLAN_ID" ] || [ "$PLAN_ID" = "null" ]; then
  echo "❌ No plans found"
  exit 1
fi

echo "✅ Plan found: $PLAN_ID"

# Step 3: Create Order
echo ""
echo "📌 Step 3: Create Order"
echo "======================="

STEP3=$(curl -s -X POST "$BASE_URL/api/v1/payments/create-order" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $AUTH_TOKEN" \
  -d "{\"planId\": \"$PLAN_ID\"}")

echo "$STEP3" | jq '.' 2>/dev/null || echo "$STEP3"

ORDER_ID=$(echo "$STEP3" | jq -r '.orderId' 2>/dev/null)

if [ -z "$ORDER_ID" ] || [ "$ORDER_ID" = "null" ]; then
  echo "❌ Failed to create order"
  exit 1
fi

echo "✅ Order created: $ORDER_ID"

# Step 4: Verify Payment
echo ""
echo "📌 Step 4: Verify Payment"
echo "========================="

PAYMENT_ID="pay_$(openssl rand -hex 6)"
RAZORPAY_SECRET="Y8X46wl0J2G7gxZvbzWcSDB9"
DATA_TO_SIGN="$ORDER_ID|$PAYMENT_ID"
SIGNATURE=$(echo -n "$DATA_TO_SIGN" | openssl dgst -sha256 -hmac "$RAZORPAY_SECRET" -hex | sed 's/^.* //')

STEP4=$(curl -s -X POST "$BASE_URL/api/v1/payments/verify" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $AUTH_TOKEN" \
  -d "{
    \"planId\": \"$PLAN_ID\",
    \"razorpayOrderId\": \"$ORDER_ID\",
    \"razorpayPaymentId\": \"$PAYMENT_ID\",
    \"razorpaySignature\": \"$SIGNATURE\"
  }")

echo "$STEP4" | jq '.' 2>/dev/null || echo "$STEP4"

if echo "$STEP4" | grep -q "Payment verified successfully"; then
  echo "✅ Payment verified"
else
  echo "❌ Payment verification failed"
fi

# ======================= NEW FEATURES =======================

# Step 5: Get Subscription Status
echo ""
echo "📌 Step 5: Get Subscription Status (NEW)"
echo "========================================="

STEP5=$(curl -s -X GET "$BASE_URL/api/v1/subscription/status" \
  -H "Authorization: Bearer $AUTH_TOKEN")

echo "$STEP5" | jq '.' 2>/dev/null || echo "$STEP5"

STATUS=$(echo "$STEP5" | jq -r '.status' 2>/dev/null)
echo "✅ Subscription status: $STATUS"

# Step 6: Get Subscription History
echo ""
echo "📌 Step 6: Get Subscription History (NEW)"
echo "=========================================="

STEP6=$(curl -s -X GET "$BASE_URL/api/v1/subscription/history" \
  -H "Authorization: Bearer $AUTH_TOKEN")

echo "$STEP6" | jq '.' 2>/dev/null || echo "$STEP6"

HISTORY_COUNT=$(echo "$STEP6" | jq 'length' 2>/dev/null)
echo "✅ Found $HISTORY_COUNT subscription(s)"

# Step 7: Cancel Subscription
echo ""
echo "📌 Step 7: Cancel Subscription"
echo "=============================="

STEP7=$(curl -s -X POST "$BASE_URL/api/v1/subscription/cancel" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $AUTH_TOKEN" \
  -d "{}")

echo "$STEP7" | jq '.' 2>/dev/null || echo "$STEP7"

CANCELLED_STATUS=$(echo "$STEP7" | jq -r '.status' 2>/dev/null)
echo "✅ Subscription cancelled: $CANCELLED_STATUS"

# Step 8: Verify Status Changed
echo ""
echo "📌 Step 8: Verify Status Changed to CANCELLED"
echo "============================================="

STEP8=$(curl -s -X GET "$BASE_URL/api/v1/subscription/status" \
  -H "Authorization: Bearer $AUTH_TOKEN")

echo "$STEP8" | jq '.' 2>/dev/null || echo "$STEP8"

FINAL_STATUS=$(echo "$STEP8" | jq -r '.status' 2>/dev/null)
echo "✅ Final status: $FINAL_STATUS"

echo ""
echo "🎉 Testing Complete!"
echo "===================="

# ============================================================================
# Individual Commands (Copy-Paste)
# ============================================================================

echo ""
echo "📚 Individual cURL Commands (Copy-Paste):"
echo "=========================================="

echo ""
echo "1️⃣  Get Subscription Status:"
echo "curl -X GET http://localhost:8085/api/v1/subscription/status \\"
echo "  -H \"Authorization: Bearer YOUR_TOKEN\""

echo ""
echo "2️⃣  Get Subscription History:"
echo "curl -X GET http://localhost:8085/api/v1/subscription/history \\"
echo "  -H \"Authorization: Bearer YOUR_TOKEN\""

echo ""
echo "3️⃣  Cancel Subscription:"
echo "curl -X POST http://localhost:8085/api/v1/subscription/cancel \\"
echo "  -H \"Content-Type: application/json\" \\"
echo "  -H \"Authorization: Bearer YOUR_TOKEN\" \\"
echo "  -d '{}'"

echo ""
echo "💡 Tips:"
echo "- Replace YOUR_TOKEN with actual auth token from Step 1"
echo "- Use Postman collection for easier testing: SubscriptionComplete.postman_collection.json"
echo "- Check application logs for scheduler execution at 02:00 UTC daily"


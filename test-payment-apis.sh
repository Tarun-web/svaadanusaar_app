#!/bin/bash

# Payment API Testing - Ready-to-Use cURL Commands
# Replace placeholders with actual values from previous responses

echo "🔧 Payment API Testing Script"
echo "=============================="

# Configuration
BASE_URL="http://localhost:8085"
PHONE="+911234567890"
OTP="123456"

echo ""
echo "📌 Step 1: OTP Verify (Get JWT Token)"
echo "========================================"

STEP1_RESPONSE=$(curl -s -X POST "$BASE_URL/api/v1/auth/otp/verify" \
  -H "Content-Type: application/json" \
  -d "{
    \"phone\": \"$PHONE\",
    \"otp\": \"$OTP\"
  }")

echo "Response:"
echo "$STEP1_RESPONSE" | jq '.' 2>/dev/null || echo "$STEP1_RESPONSE"

# Extract token and userId
AUTH_TOKEN=$(echo "$STEP1_RESPONSE" | jq -r '.token' 2>/dev/null)
USER_ID=$(echo "$STEP1_RESPONSE" | jq -r '.userId' 2>/dev/null)

if [ -z "$AUTH_TOKEN" ] || [ "$AUTH_TOKEN" = "null" ]; then
  echo "❌ Failed to get auth token. Exiting."
  exit 1
fi

echo ""
echo "✅ Token: $AUTH_TOKEN"
echo "✅ UserId: $USER_ID"

# Save for later use
export AUTH_TOKEN
export USER_ID

echo ""
echo "📌 Step 2: Get Active Subscription Plans"
echo "==========================================="

STEP2_RESPONSE=$(curl -s -X GET "$BASE_URL/api/v1/subscription/plans/active")

echo "Response:"
echo "$STEP2_RESPONSE" | jq '.' 2>/dev/null || echo "$STEP2_RESPONSE"

# Extract first plan ID
PLAN_ID=$(echo "$STEP2_RESPONSE" | jq -r '.[0].id' 2>/dev/null)
PLAN_PRICE=$(echo "$STEP2_RESPONSE" | jq -r '.[0].price' 2>/dev/null)

if [ -z "$PLAN_ID" ] || [ "$PLAN_ID" = "null" ]; then
  echo "❌ No plans found. Exiting."
  exit 1
fi

echo ""
echo "✅ Plan ID: $PLAN_ID"
echo "✅ Plan Price: ₹$PLAN_PRICE"

export PLAN_ID

echo ""
echo "📌 Step 3: Create Order (Razorpay)"
echo "===================================="

STEP3_RESPONSE=$(curl -s -X POST "$BASE_URL/api/v1/payments/create-order" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $AUTH_TOKEN" \
  -d "{
    \"planId\": \"$PLAN_ID\"
  }")

echo "Response:"
echo "$STEP3_RESPONSE" | jq '.' 2>/dev/null || echo "$STEP3_RESPONSE"

# Extract order details
ORDER_ID=$(echo "$STEP3_RESPONSE" | jq -r '.orderId' 2>/dev/null)
ORDER_AMOUNT=$(echo "$STEP3_RESPONSE" | jq -r '.amount' 2>/dev/null)

if [ -z "$ORDER_ID" ] || [ "$ORDER_ID" = "null" ]; then
  echo "❌ Failed to create order. Exiting."
  exit 1
fi

echo ""
echo "✅ Order ID: $ORDER_ID"
echo "✅ Amount: $ORDER_AMOUNT paise (₹$((ORDER_AMOUNT / 100)))"

export ORDER_ID
export ORDER_AMOUNT

echo ""
echo "📌 Step 4: Generate Payment ID and Signature"
echo "=============================================="

# Generate a fake payment ID
PAYMENT_ID="pay_$(openssl rand -hex 6)"

# Get razorpay_secret (hardcoded for testing)
RAZORPAY_SECRET="Y8X46wl0J2G7gxZvbzWcSDB9"

# Create the data string for signature
DATA_TO_SIGN="$ORDER_ID|$PAYMENT_ID"

# Generate HMAC-SHA256 signature
SIGNATURE=$(echo -n "$DATA_TO_SIGN" | openssl dgst -sha256 -hmac "$RAZORPAY_SECRET" -hex | sed 's/^.* //')

echo ""
echo "✅ Payment ID: $PAYMENT_ID"
echo "✅ Data to Sign: $DATA_TO_SIGN"
echo "✅ Signature: $SIGNATURE"

export PAYMENT_ID
export SIGNATURE

echo ""
echo "📌 Step 5: Verify Payment"
echo "========================="

STEP5_RESPONSE=$(curl -s -X POST "$BASE_URL/api/v1/payments/verify" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $AUTH_TOKEN" \
  -d "{
    \"planId\": \"$PLAN_ID\",
    \"razorpayOrderId\": \"$ORDER_ID\",
    \"razorpayPaymentId\": \"$PAYMENT_ID\",
    \"razorpaySignature\": \"$SIGNATURE\"
  }")

echo "Response:"
echo "$STEP5_RESPONSE" | jq '.' 2>/dev/null || echo "$STEP5_RESPONSE"

if echo "$STEP5_RESPONSE" | grep -q "Payment verified successfully"; then
  echo ""
  echo "✅ Payment verified successfully!"
  echo "✅ Subscription activated!"
else
  echo ""
  echo "❌ Payment verification failed"
  exit 1
fi

echo ""
echo "📌 Step 6: Cancel Subscription (Optional)"
echo "========================================="

read -p "Do you want to cancel the subscription? (y/n): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
  STEP6_RESPONSE=$(curl -s -X POST "$BASE_URL/api/v1/subscription/cancel" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $AUTH_TOKEN" \
    -d "{}")

  echo "Response:"
  echo "$STEP6_RESPONSE" | jq '.' 2>/dev/null || echo "$STEP6_RESPONSE"

  if echo "$STEP6_RESPONSE" | grep -q "CANCELLED"; then
    echo ""
    echo "✅ Subscription cancelled!"
  fi
fi

echo ""
echo "🎉 Testing Complete!"
echo "===================="
echo ""
echo "Summary:"
echo "- Phone: $PHONE"
echo "- Auth Token: ${AUTH_TOKEN:0:20}..."
echo "- Plan ID: $PLAN_ID"
echo "- Order ID: $ORDER_ID"
echo "- Payment ID: $PAYMENT_ID"
echo ""

# ============================================================================
# Individual cURL Commands (if you want to run them manually one by one)
# ============================================================================

echo ""
echo "📚 Individual Commands (Copy-Paste):"
echo "===================================="
echo ""

echo "1️⃣  OTP Verify:"
echo "curl -X POST $BASE_URL/api/v1/auth/otp/verify \\"
echo "  -H 'Content-Type: application/json' \\"
echo "  -d '{"
echo "    \"phone\": \"$PHONE\","
echo "    \"otp\": \"$OTP\""
echo "  }'"
echo ""

echo "2️⃣  Get Active Plans:"
echo "curl -X GET $BASE_URL/api/v1/subscription/plans/active"
echo ""

if [ ! -z "$AUTH_TOKEN" ]; then
  echo "3️⃣  Create Order (replace PLAN_ID):"
  echo "curl -X POST $BASE_URL/api/v1/payments/create-order \\"
  echo "  -H 'Content-Type: application/json' \\"
  echo "  -H 'Authorization: Bearer $AUTH_TOKEN' \\"
  echo "  -d '{"
  echo "    \"planId\": \"PLAN_ID\""
  echo "  }'"
  echo ""

  echo "4️⃣  Verify Payment (replace ORDER_ID, PAYMENT_ID, SIGNATURE):"
  echo "curl -X POST $BASE_URL/api/v1/payments/verify \\"
  echo "  -H 'Content-Type: application/json' \\"
  echo "  -H 'Authorization: Bearer $AUTH_TOKEN' \\"
  echo "  -d '{"
  echo "    \"planId\": \"PLAN_ID\","
  echo "    \"razorpayOrderId\": \"ORDER_ID\","
  echo "    \"razorpayPaymentId\": \"PAYMENT_ID\","
  echo "    \"razorpaySignature\": \"SIGNATURE\""
  echo "  }'"
  echo ""

  echo "5️⃣  Cancel Subscription:"
  echo "curl -X POST $BASE_URL/api/v1/subscription/cancel \\"
  echo "  -H 'Content-Type: application/json' \\"
  echo "  -H 'Authorization: Bearer $AUTH_TOKEN' \\"
  echo "  -d '{}'"
fi



#!/bin/bash

# Verify Payment API Testing Script
# This script tests the complete verify payment flow with automatic signature generation

echo "🔐 Verify Payment API Testing"
echo "=============================="

# Configuration
BASE_URL="http://localhost:8085"
PHONE="+911234567890"
OTP="123456"
RAZORPAY_SECRET="Y8X46wl0J2G7gxZvbzWcSDB9"

# ============================================================================
# STEP 1: OTP Verify (Get Token)
# ============================================================================

echo ""
echo "📌 Step 1: OTP Verify"
echo "====================="

STEP1=$(curl -s -X POST "$BASE_URL/api/v1/auth/otp/verify" \
  -H "Content-Type: application/json" \
  -d "{\"phone\":\"$PHONE\",\"otp\":\"$OTP\"}")

echo "Response:"
echo "$STEP1" | jq '.' 2>/dev/null || echo "$STEP1"

AUTH_TOKEN=$(echo "$STEP1" | jq -r '.token' 2>/dev/null)
USER_ID=$(echo "$STEP1" | jq -r '.userId' 2>/dev/null)

if [ -z "$AUTH_TOKEN" ] || [ "$AUTH_TOKEN" = "null" ]; then
  echo "❌ Failed to get auth token"
  exit 1
fi

echo ""
echo "✅ Token: ${AUTH_TOKEN:0:20}..."
echo "✅ UserId: $USER_ID"

# ============================================================================
# STEP 2: Get Plans
# ============================================================================

echo ""
echo "📌 Step 2: Get Active Plans"
echo "==========================="

STEP2=$(curl -s -X GET "$BASE_URL/api/v1/subscription/plans/active")

echo "Response:"
echo "$STEP2" | jq '.' 2>/dev/null || echo "$STEP2"

PLAN_ID=$(echo "$STEP2" | jq -r '.[0].id' 2>/dev/null)

if [ -z "$PLAN_ID" ] || [ "$PLAN_ID" = "null" ]; then
  echo "❌ No plans found"
  exit 1
fi

echo ""
echo "✅ Plan ID: $PLAN_ID"

# ============================================================================
# STEP 3: Create Order
# ============================================================================

echo ""
echo "📌 Step 3: Create Order"
echo "======================="

STEP3=$(curl -s -X POST "$BASE_URL/api/v1/payments/create-order" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $AUTH_TOKEN" \
  -d "{\"planId\":\"$PLAN_ID\"}")

echo "Response:"
echo "$STEP3" | jq '.' 2>/dev/null || echo "$STEP3"

ORDER_ID=$(echo "$STEP3" | jq -r '.orderId' 2>/dev/null)
ORDER_AMOUNT=$(echo "$STEP3" | jq -r '.amount' 2>/dev/null)

if [ -z "$ORDER_ID" ] || [ "$ORDER_ID" = "null" ]; then
  echo "❌ Failed to create order"
  exit 1
fi

echo ""
echo "✅ Order ID: $ORDER_ID"
echo "✅ Amount: ₹$((ORDER_AMOUNT / 100))"

# ============================================================================
# STEP 4: Generate Fake Payment Data & Signature
# ============================================================================

echo ""
echo "📌 Step 4: Generate Payment Data & Signature"
echo "============================================"

# Generate fake payment ID
PAYMENT_ID="pay_$(openssl rand -hex 6)"
echo "Generated Payment ID: $PAYMENT_ID"

# Create data string to sign
DATA_TO_SIGN="$ORDER_ID|$PAYMENT_ID"
echo "Data to sign: $DATA_TO_SIGN"

# Generate HMAC-SHA256 signature
SIGNATURE=$(echo -n "$DATA_TO_SIGN" | \
  openssl dgst -sha256 -hmac "$RAZORPAY_SECRET" -hex | \
  sed 's/^.* //')

echo "Generated Signature: $SIGNATURE"

echo ""
echo "✅ Payment ID: $PAYMENT_ID"
echo "✅ Signature: ${SIGNATURE:0:20}..."

# ============================================================================
# STEP 5: Verify Payment
# ============================================================================

echo ""
echo "📌 Step 5: Verify Payment (MAIN TEST)"
echo "====================================="

echo ""
echo "Request Payload:"
echo "================"
cat <<EOF
{
  "planId": "$PLAN_ID",
  "razorpayOrderId": "$ORDER_ID",
  "razorpayPaymentId": "$PAYMENT_ID",
  "razorpaySignature": "$SIGNATURE"
}
EOF

echo ""
echo "Sending request..."
echo ""

STEP5=$(curl -s -X POST "$BASE_URL/api/v1/payments/verify" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $AUTH_TOKEN" \
  -d "{
    \"planId\":\"$PLAN_ID\",
    \"razorpayOrderId\":\"$ORDER_ID\",
    \"razorpayPaymentId\":\"$PAYMENT_ID\",
    \"razorpaySignature\":\"$SIGNATURE\"
  }")

echo "Response:"
echo "$STEP5" | jq '.' 2>/dev/null || echo "$STEP5"

if echo "$STEP5" | grep -q "Payment verified successfully"; then
  echo ""
  echo "✅ Payment verified successfully!"
  echo "✅ Subscription should now be ACTIVE"
else
  echo ""
  echo "❌ Payment verification failed"
  exit 1
fi

# ============================================================================
# STEP 6: Verify Status (Confirm subscription is active)
# ============================================================================

echo ""
echo "📌 Step 6: Verify Subscription Status"
echo "====================================="

STEP6=$(curl -s -X GET "$BASE_URL/api/v1/subscription/status" \
  -H "Authorization: Bearer $AUTH_TOKEN")

echo "Response:"
echo "$STEP6" | jq '.' 2>/dev/null || echo "$STEP6"

STATUS=$(echo "$STEP6" | jq -r '.status' 2>/dev/null)

if [ "$STATUS" = "ACTIVE" ]; then
  echo ""
  echo "✅ Subscription is ACTIVE!"
else
  echo ""
  echo "⚠️  Status: $STATUS"
fi

# ============================================================================
# SUMMARY
# ============================================================================

echo ""
echo "🎉 TESTING COMPLETE!"
echo "===================="
echo ""
echo "Summary:"
echo "--------"
echo "• Phone: $PHONE"
echo "• Token: ${AUTH_TOKEN:0:20}..."
echo "• Plan: $PLAN_ID"
echo "• Order ID: $ORDER_ID"
echo "• Payment ID: $PAYMENT_ID"
echo "• Signature: ${SIGNATURE:0:30}..."
echo ""

# ============================================================================
# DATABASE VERIFICATION COMMANDS
# ============================================================================

echo ""
echo "📊 To verify in database, run these SQL commands:"
echo "=================================================="
echo ""
echo "1. Check Payment Record:"
echo "   SELECT * FROM payments WHERE user_id = '$USER_ID' ORDER BY created_at DESC LIMIT 1;"
echo ""
echo "2. Check Subscription Record:"
echo "   SELECT * FROM user_subscriptions WHERE user_id = '$USER_ID' ORDER BY created_at DESC LIMIT 1;"
echo ""

# ============================================================================
# TESTING ERROR CASES
# ============================================================================

echo ""
echo "🧪 Optional: Test Error Cases"
echo "============================="
echo ""
echo "To test INVALID SIGNATURE:"
echo "1. Replace signature with: 'WRONG_SIGNATURE_12345'"
echo "2. Run verify endpoint"
echo "3. Should get 400 error: 'Invalid signature'"
echo ""
echo "To test INVALID PLAN:"
echo "1. Replace planId with: 'non_existent_plan'"
echo "2. Run verify endpoint (step 5 above)"
echo "3. Should get error about plan not found"
echo ""

echo "✅ Script completed!"


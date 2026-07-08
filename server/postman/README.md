Postman collection and environment for testing Subscription & Payment flows

Checklist
- [x] OTP login (POST /api/v1/auth/otp/verify) — use OTP `123456`
- [x] Get active plans (GET /api/v1/subscription/plans/active)
- [x] Create order (POST /api/v1/payments/create-order) — requires Bearer token
- [x] Verify payment (POST /api/v1/payments/verify) — simulated signature computed in pre-request script
- [x] Cancel subscription (POST /api/v1/subscription/cancel)

How it works
1. Start your Spring Boot app locally (default port 8085). Ensure DB is running and migrations applied.

Run the app:
```bash
./mvnw spring-boot:run
```

2. Open Postman and import the collection file `SubscriptionPayment.postman_collection.json` and environment `SubscriptionPayment.postman_environment.json` located in this folder.

3. Select the environment `DietApp Local` and set the `baseUrl` if different.

4. Run the requests in order using the Collection Runner or run via newman for automated regression.

Run with newman (install if you don't have it):
```bash
npm install -g newman
newman run postman/SubscriptionPayment.postman_collection.json -e postman/SubscriptionPayment.postman_environment.json --delay-request 500
```

Notes and troubleshooting
- OTP is hardcoded in the app as `123456` for testing.
- The Verify Payment step simulates a Razorpay payment by generating a fake payment id and computing an HMAC-SHA256 signature using the `razorpay_secret` environment value. The `razorpay_secret` value is read from your `application.properties`.
- If your Razorpay keys are not configured or you prefer to bypass external calls, the collection simulates only the verification signature — the app still uses the `razorpayClient` to create an order (so your `razorpay.key.id` and `razorpay.key.secret` must be valid to create an order).
- To run fully offline (without contacting Razorpay), you can stub `RazorpayClient` in tests or modify `RazorpayConfig` to return a fake client. If you want, I can add a test profile that mocks Razorpay for CI/regression runs.

Contact
If you want, I can also:
- Add a Maven profile and test that runs these flows automatically using a headless HTTP client (RestAssured) and an in-memory DB.
- Add a mock Razorpay client for fully offline regression testing.


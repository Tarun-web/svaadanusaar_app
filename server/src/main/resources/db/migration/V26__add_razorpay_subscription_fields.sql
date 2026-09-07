ALTER TABLE subscription_plans
    ADD COLUMN razorpay_plan_id VARCHAR(255);

ALTER TABLE user_subscriptions
    ADD COLUMN razorpay_subscription_id VARCHAR(255);
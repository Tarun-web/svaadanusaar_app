ALTER TABLE payments
    ADD COLUMN plan_id TEXT REFERENCES subscription_plans(id);

ALTER TABLE payments
    ADD COLUMN razorpay_subscription_id VARCHAR(255);

CREATE UNIQUE INDEX ux_payments_razorpay_subscription_id
    ON payments(razorpay_subscription_id)
    WHERE razorpay_subscription_id IS NOT NULL;
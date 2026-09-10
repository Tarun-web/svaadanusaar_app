CREATE TABLE payment_webhook_events (
                                        id UUID PRIMARY KEY,
                                        provider_event_id VARCHAR(255) NOT NULL UNIQUE,
                                        event_type VARCHAR(100) NOT NULL,
                                        received_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
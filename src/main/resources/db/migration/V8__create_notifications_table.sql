CREATE TABLE notifications (
    notification_id UUID PRIMARY KEY,
    user_id         UUID        NOT NULL REFERENCES users (user_id),
    type            VARCHAR(64) NOT NULL,
    payload         JSONB       NOT NULL DEFAULT '{}'::jsonb,
    is_read         BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_user_created ON notifications (user_id, created_at DESC);
CREATE INDEX idx_notifications_user_unread ON notifications (user_id, is_read) WHERE is_read = FALSE;

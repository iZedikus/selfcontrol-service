CREATE TABLE audit_events
(
    event_id       UUID PRIMARY KEY,
    actor_user_id  UUID REFERENCES users (user_id),
    target_user_id UUID REFERENCES users (user_id),
    action         VARCHAR(128) NOT NULL,
    entity_type    VARCHAR(128) NOT NULL,
    entity_id      VARCHAR(255),
    payload        TEXT,
    created_at     TIMESTAMPTZ  NOT NULL,
    result         VARCHAR(64)  NOT NULL
);

CREATE INDEX idx_audit_events_target_created_at ON audit_events (target_user_id, created_at DESC);
CREATE INDEX idx_audit_events_actor_created_at ON audit_events (actor_user_id, created_at DESC);

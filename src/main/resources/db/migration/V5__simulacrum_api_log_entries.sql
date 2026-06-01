CREATE TABLE simulacrum_api_log_entries
(
    id              UUID PRIMARY KEY,
    created_at      TIMESTAMPTZ  NOT NULL,
    method          VARCHAR(16)   NOT NULL,
    path            VARCHAR(2000) NOT NULL,
    request_body    TEXT,
    response_status INTEGER,
    response_body   TEXT,
    error_message   TEXT,
    correlation_id  UUID         NOT NULL,
    operation_type  VARCHAR(128) NOT NULL,
    user_id         UUID REFERENCES users (user_id)
);

CREATE INDEX idx_simulacrum_api_log_created_at ON simulacrum_api_log_entries (created_at DESC);
CREATE INDEX idx_simulacrum_api_log_user_created_at ON simulacrum_api_log_entries (user_id, created_at DESC);
CREATE INDEX idx_simulacrum_api_log_operation_created_at ON simulacrum_api_log_entries (operation_type, created_at DESC);
CREATE INDEX idx_simulacrum_api_log_status_created_at ON simulacrum_api_log_entries (response_status, created_at DESC);
CREATE INDEX idx_simulacrum_api_log_correlation_id ON simulacrum_api_log_entries (correlation_id);

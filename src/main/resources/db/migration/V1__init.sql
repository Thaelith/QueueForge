CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE jobs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    queue_name VARCHAR(100) NOT NULL DEFAULT 'default',
    type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(40) NOT NULL,
    priority INT NOT NULL DEFAULT 50,
    run_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    attempts INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 3,
    locked_by VARCHAR(100),
    locked_until TIMESTAMPTZ,
    idempotency_key VARCHAR(200),
    last_error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ,
    dead_lettered_at TIMESTAMPTZ
);

CREATE UNIQUE INDEX ux_jobs_idempotency_key ON jobs (idempotency_key) WHERE idempotency_key IS NOT NULL;
CREATE INDEX ix_jobs_available ON jobs (status, run_at, priority DESC, created_at) WHERE status IN ('PENDING', 'RETRY_SCHEDULED');
CREATE INDEX ix_jobs_locked_until ON jobs (locked_until) WHERE status = 'RUNNING';
CREATE INDEX ix_jobs_type_status ON jobs (type, status);
CREATE INDEX ix_jobs_queue_name ON jobs (queue_name);

CREATE TABLE job_events (
    id BIGSERIAL PRIMARY KEY,
    job_id UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    event_type VARCHAR(80) NOT NULL,
    message TEXT,
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_job_events_job_id ON job_events (job_id);

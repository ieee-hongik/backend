CREATE TABLE IF NOT EXISTS automatic_job_failure (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    job_type VARCHAR(80) NOT NULL,
    target_type VARCHAR(80),
    target_id BIGINT,
    failed_at_utc DATETIME(6) NOT NULL,
    error_type VARCHAR(255) NOT NULL,
    error_summary VARCHAR(255) NOT NULL
);

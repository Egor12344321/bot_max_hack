CREATE TABLE applicant_sessions (
    id UUID PRIMARY KEY,
    max_user_id BIGINT NOT NULL UNIQUE,
    language VARCHAR(2) NOT NULL,
    country_code VARCHAR(16),
    track VARCHAR(32) NOT NULL,
    state VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE applicant_session_ege_scores (
    session_id UUID NOT NULL REFERENCES applicant_sessions (id) ON DELETE CASCADE,
    subject_id VARCHAR(64) NOT NULL,
    score INTEGER NOT NULL CHECK (score BETWEEN 0 AND 100),
    PRIMARY KEY (session_id, subject_id)
);

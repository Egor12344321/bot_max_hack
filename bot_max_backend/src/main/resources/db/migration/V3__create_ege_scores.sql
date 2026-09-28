CREATE TABLE ege_scores (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    subject_id VARCHAR(50) NOT NULL REFERENCES subjects(id),
    score INTEGER NOT NULL CHECK (score >= 0 AND score <= 100),
    UNIQUE (user_id, subject_id)
);

CREATE INDEX idx_ege_scores_user_id ON ege_scores(user_id);
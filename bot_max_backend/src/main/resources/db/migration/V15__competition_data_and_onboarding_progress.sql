-- Не подменяем неизвестные места нулями и проходные квот проходными общего конкурса.
CREATE TABLE program_competitions (
    program_id VARCHAR(80) NOT NULL REFERENCES programs(id) ON DELETE CASCADE,
    competition_type VARCHAR(30) NOT NULL CHECK (competition_type IN ('general', 'special_quota', 'separate_quota', 'target_quota')),
    seats INTEGER CHECK (seats >= 0),
    passing_score INTEGER CHECK (passing_score >= 0),
    previous_year INTEGER,
    data_source VARCHAR(30) NOT NULL DEFAULT 'demo',
    PRIMARY KEY (program_id, competition_type)
);
INSERT INTO program_competitions (program_id, competition_type, passing_score, previous_year)
SELECT id, 'general', passing_score_previous_year, passing_score_year FROM programs;
-- Право на эти конкурсы не означает известное число мест или автоматическое зачисление.
INSERT INTO program_competitions (program_id, competition_type)
SELECT id, competition_type FROM programs CROSS JOIN
    (VALUES ('special_quota'), ('separate_quota'), ('target_quota')) AS types(competition_type);

ALTER TABLE users ADD COLUMN mini_app_onboarding_complete BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN max_score_deficit INTEGER NOT NULL DEFAULT 15
    CHECK (max_score_deficit IN (0, 10, 15, 20));
ALTER TABLE users ADD COLUMN preferred_competition_type VARCHAR(30)
    CHECK (preferred_competition_type IN ('general', 'special_quota', 'separate_quota', 'target_quota'));

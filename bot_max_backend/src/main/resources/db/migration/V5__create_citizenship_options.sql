CREATE TABLE citizenship_options (
    code VARCHAR(5) PRIMARY KEY,
    name_ru VARCHAR(100) NOT NULL,
    name_kk VARCHAR(100),
    name_ky VARCHAR(100),
    group_name VARCHAR(20) NOT NULL CHECK (group_name IN ('eaeu', 'other'))
);
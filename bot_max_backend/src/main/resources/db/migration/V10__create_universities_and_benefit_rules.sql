CREATE TABLE universities (
	id VARCHAR(50) PRIMARY KEY,
	name VARCHAR(200) NOT NULL,
	short_name VARCHAR(50) NOT NULL,
	city VARCHAR(100) NOT NULL,
	achievement_points_max INTEGER NOT NULL DEFAULT 10
);

CREATE TABLE programs (
	id VARCHAR(80) PRIMARY KEY,
	university_id VARCHAR(50) NOT NULL REFERENCES universities(id),
	code VARCHAR(10),
	name VARCHAR(200) NOT NULL,
	passing_score_previous_year INTEGER
);

CREATE INDEX idx_programs_university_id ON programs(university_id);

-- Предметы ЕГЭ направления. Предметы с одинаковым choice_group взаимозаменяемы
-- («информатика или физика»): в сумму идёт лучший из них.
CREATE TABLE program_subjects (
	program_id VARCHAR(80) NOT NULL REFERENCES programs(id) ON DELETE CASCADE,
	subject_id VARCHAR(50) NOT NULL REFERENCES subjects(id),
	choice_group INTEGER,
	PRIMARY KEY (program_id, subject_id)
);

-- Профили олимпиад, которые вуз считает соответствующими направлению.
-- БВИ и 100 баллов даются только по соответствующим профилям.
CREATE TABLE program_olympiad_profiles (
	program_id VARCHAR(80) NOT NULL REFERENCES programs(id) ON DELETE CASCADE,
	profile VARCHAR(50) NOT NULL,
	PRIMARY KEY (program_id, profile)
);

-- Что даёт диплом олимпиады. Пустое поле означает «любое значение»:
--   university_id — во всех вузах (так работает ВсОШ);
--   program_id    — на всех направлениях вуза, которым подходит профиль;
--   olympiad_id   — любая олимпиада этого профиля;
--   degree        — и победителю, и призёру.
-- max_level — худший подходящий уровень: 1 = только I уровень, 2 = I и II, 3 = любой.
-- min_ege_score — сколько нужно набрать на ЕГЭ по предмету профиля, чтобы подтвердить диплом.
-- Если дипломом подходят несколько правил, применяется самое выгодное из тех, для которых хватает ЕГЭ.
CREATE TABLE olympiad_benefit_rules (
	id BIGSERIAL PRIMARY KEY,
	university_id VARCHAR(50) REFERENCES universities(id),
	program_id VARCHAR(80) REFERENCES programs(id),
	olympiad_id VARCHAR(50) REFERENCES olympiads(id),
	profile VARCHAR(50) NOT NULL,
	vsosh BOOLEAN NOT NULL DEFAULT FALSE,
	max_level INTEGER CHECK (max_level BETWEEN 1 AND 3),
	degree VARCHAR(10) CHECK (degree IN ('WINNER', 'PRIZE')),
	benefit VARCHAR(20) NOT NULL CHECK (benefit IN ('BVI', 'SCORE_100', 'ACHIEVEMENT_POINTS')),
	points INTEGER CHECK (points > 0),
	min_ege_score INTEGER CHECK (min_ege_score BETWEEN 0 AND 100),
	CHECK ((benefit = 'ACHIEVEMENT_POINTS') = (points IS NOT NULL))
);

-- Баллы, которые вуз даёт за индивидуальные достижения из справочника achievements.
-- Достижения без строки здесь вуз не учитывает.
CREATE TABLE achievement_point_rules (
	university_id VARCHAR(50) NOT NULL REFERENCES universities(id),
	achievement_id VARCHAR(50) NOT NULL REFERENCES achievements(id),
	points INTEGER NOT NULL CHECK (points > 0),
	PRIMARY KEY (university_id, achievement_id)
);

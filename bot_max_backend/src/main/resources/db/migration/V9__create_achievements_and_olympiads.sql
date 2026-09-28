-- Справочник индивидуальных достижений (ИД). Раньше жил в catalog/achievements.json,
-- переехал в БД, чтобы на него могли ссылаться правила вузов (achievement_point_rules).
-- Из достижений с одинаковой exclusive_group можно выбрать только одно (медаль I или II степени).
CREATE TABLE achievements (
	id VARCHAR(50) PRIMARY KEY,
	name VARCHAR(150) NOT NULL,
	description VARCHAR(300),
	exclusive_group VARCHAR(50),
	sort_order INTEGER NOT NULL DEFAULT 0
);

INSERT INTO achievements (id, name, description, exclusive_group, sort_order) VALUES
	('medal_gold', 'Медаль «За особые успехи в учении» I степени', 'Золотая медаль и аттестат с отличием', 'medal', 1),
	('medal_silver', 'Медаль «За особые успехи в учении» II степени', 'Серебряная медаль и аттестат с отличием', 'medal', 2),
	('gto_gold', 'Золотой знак ГТО', 'Засчитывается, если знак получен в текущей или прошлой возрастной группе', NULL, 3),
	('volunteering', 'Волонтёрская деятельность', 'Обычно нужно от 100 подтверждённых часов', NULL, 4),
	('essay', 'Итоговое сочинение', 'Учитывают не все вузы', NULL, 5);

-- Старые id из тестового JSON (school_medal, olympiad) больше не существуют.
DELETE FROM user_achievements WHERE achievement_id NOT IN (SELECT id FROM achievements);

ALTER TABLE user_achievements
	ADD CONSTRAINT fk_user_achievements_achievement FOREIGN KEY (achievement_id) REFERENCES achievements(id);

-- Олимпиады: ВсОШ и олимпиады из перечня РСОШ.
CREATE TABLE olympiads (
	id VARCHAR(50) PRIMARY KEY,
	name VARCHAR(200) NOT NULL,
	is_vsosh BOOLEAN NOT NULL DEFAULT FALSE,
	list_number INTEGER
);

-- Профиль олимпиады (математика, информатика...) в конкретном учебном году.
-- Уровень I–III присваивается профилю, а не олимпиаде целиком, и может меняться от года к году.
-- subject_id — предмет ЕГЭ, которым подтверждают диплом и по которому дают 100 баллов.
-- level у ВсОШ пустой: у неё нет уровня.
CREATE TABLE olympiad_profiles (
	id VARCHAR(80) PRIMARY KEY,
	olympiad_id VARCHAR(50) NOT NULL REFERENCES olympiads(id),
	profile VARCHAR(50) NOT NULL,
	name VARCHAR(100) NOT NULL,
	subject_id VARCHAR(50) NOT NULL REFERENCES subjects(id),
	level INTEGER CHECK (level BETWEEN 1 AND 3),
	olympiad_year INTEGER NOT NULL
);

CREATE INDEX idx_olympiad_profiles_olympiad_id ON olympiad_profiles(olympiad_id);

CREATE TABLE user_olympiad_diplomas (
	user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
	profile_id VARCHAR(80) NOT NULL REFERENCES olympiad_profiles(id),
	degree VARCHAR(10) NOT NULL CHECK (degree IN ('WINNER', 'PRIZE')),
	PRIMARY KEY (user_id, profile_id)
);

-- Перечень олимпиад школьников на 2025/26 учебный год (приказ Минобрнауки № 669 от 30.08.2025).
-- Номера и уровни сверены с https://rsr-olymp.ru. Взяты популярные олимпиады
-- по математике, информатике и физике: этого хватает для IT-направлений из демо-вузов.
INSERT INTO olympiads (id, name, is_vsosh, list_number) VALUES
	('vsosh', 'Всероссийская олимпиада школьников (заключительный этап)', TRUE, NULL),
	('vao', 'Вузовско-академическая олимпиада по информатике', FALSE, 15),
	('ai-olympiad', 'Всероссийская олимпиада по искусственному интеллекту', FALSE, 7),
	('vysshaya-proba', 'Всероссийская олимпиада школьников «Высшая проба»', FALSE, 8),
	('engineering', 'Инженерная олимпиада школьников', FALSE, 18),
	('innopolis-open', 'Международная олимпиада «Innopolis Open»', FALSE, 22),
	('mosh', 'Московская олимпиада школьников', FALSE, 37),
	('kurchatov', 'Олимпиада школьников «Курчатов»', FALSE, 43),
	('lomonosov', 'Олимпиада школьников «Ломоносов»', FALSE, 50),
	('pvg', 'Олимпиада школьников «Покори Воробьёвы горы!»', FALSE, 52),
	('phystech', 'Олимпиада школьников «Физтех»', FALSE, 54),
	('step-into-future', 'Олимпиада школьников «Шаг в будущее»', FALSE, 55),
	('inf-prog', 'Олимпиада школьников по информатике и программированию', FALSE, 56),
	('technocup', 'Олимпиада школьников по программированию «ТехноКубок»', FALSE, 57),
	('spbu', 'Олимпиада школьников Санкт-Петербургского государственного университета', FALSE, 59),
	('itmo-open', 'Открытая олимпиада школьников', FALSE, 64),
	('open-programming', 'Открытая олимпиада школьников по программированию', FALSE, 65),
	('rosatom', 'Отраслевая физико-математическая олимпиада школьников «Росатом»', FALSE, 70),
	('spb-olympiad', 'Санкт-Петербургская олимпиада школьников', FALSE, 75),
	('tournament-of-towns', 'Турнир городов', FALSE, 81),
	('belchonok', 'Университетская олимпиада школьников «Бельчонок»', FALSE, 83);

INSERT INTO olympiad_profiles (id, olympiad_id, profile, name, subject_id, level, olympiad_year) VALUES
	('vsosh-math-2026', 'vsosh', 'math', 'математика', 'math-profile', NULL, 2026),
	('vsosh-informatics-2026', 'vsosh', 'informatics', 'информатика', 'informatics', NULL, 2026),
	('vsosh-physics-2026', 'vsosh', 'physics', 'физика', 'physics', NULL, 2026),

	('vao-informatics-2026', 'vao', 'informatics', 'информатика', 'informatics', 1, 2026),
	('ai-olympiad-ai-2026', 'ai-olympiad', 'ai', 'искусственный интеллект', 'informatics', 2, 2026),

	('vysshaya-proba-informatics-2026', 'vysshaya-proba', 'informatics', 'информатика', 'informatics', 1, 2026),
	('vysshaya-proba-math-2026', 'vysshaya-proba', 'math', 'математика', 'math-profile', 1, 2026),
	('vysshaya-proba-physics-2026', 'vysshaya-proba', 'physics', 'физика', 'physics', 2, 2026),

	('engineering-physics-2026', 'engineering', 'physics', 'физика', 'physics', 1, 2026),

	('innopolis-open-informatics-2026', 'innopolis-open', 'informatics', 'информатика', 'informatics', 2, 2026),
	('innopolis-open-math-2026', 'innopolis-open', 'math', 'математика', 'math-profile', 2, 2026),
	('innopolis-open-ai-2026', 'innopolis-open', 'ai', 'искусственный интеллект', 'informatics', 3, 2026),

	('mosh-informatics-2026', 'mosh', 'informatics', 'информатика', 'informatics', 1, 2026),
	('mosh-math-2026', 'mosh', 'math', 'математика', 'math-profile', 1, 2026),
	('mosh-physics-2026', 'mosh', 'physics', 'физика', 'physics', 1, 2026),

	('kurchatov-math-2026', 'kurchatov', 'math', 'математика', 'math-profile', 2, 2026),
	('kurchatov-physics-2026', 'kurchatov', 'physics', 'физика', 'physics', 2, 2026),

	('lomonosov-informatics-2026', 'lomonosov', 'informatics', 'информатика', 'informatics', 2, 2026),
	('lomonosov-math-2026', 'lomonosov', 'math', 'математика', 'math-profile', 1, 2026),
	('lomonosov-physics-2026', 'lomonosov', 'physics', 'физика', 'physics', 1, 2026),

	('pvg-math-2026', 'pvg', 'math', 'математика', 'math-profile', 1, 2026),
	('pvg-physics-2026', 'pvg', 'physics', 'физика', 'physics', 1, 2026),

	('phystech-informatics-2026', 'phystech', 'informatics', 'информатика и программирование', 'informatics', 3, 2026),
	('phystech-math-2026', 'phystech', 'math', 'математика', 'math-profile', 2, 2026),
	('phystech-physics-2026', 'phystech', 'physics', 'физика', 'physics', 1, 2026),

	('step-into-future-informatics-2026', 'step-into-future', 'informatics', 'информатика', 'informatics', 3, 2026),
	('step-into-future-math-2026', 'step-into-future', 'math', 'математика', 'math-profile', 3, 2026),
	('step-into-future-physics-2026', 'step-into-future', 'physics', 'физика', 'physics', 2, 2026),

	('inf-prog-informatics-2026', 'inf-prog', 'informatics', 'информатика', 'informatics', 1, 2026),
	('technocup-informatics-2026', 'technocup', 'informatics', 'информатика', 'informatics', 2, 2026),

	('spbu-informatics-2026', 'spbu', 'informatics', 'информатика', 'informatics', 1, 2026),
	('spbu-math-2026', 'spbu', 'math', 'математика', 'math-profile', 1, 2026),
	('spbu-physics-2026', 'spbu', 'physics', 'физика', 'physics', 2, 2026),

	('itmo-open-informatics-2026', 'itmo-open', 'informatics', 'информатика', 'informatics', 1, 2026),
	('itmo-open-math-2026', 'itmo-open', 'math', 'математика', 'math-profile', 3, 2026),
	('itmo-open-physics-2026', 'itmo-open', 'physics', 'физика', 'physics', 3, 2026),

	('open-programming-informatics-2026', 'open-programming', 'informatics', 'информатика', 'informatics', 1, 2026),

	('rosatom-informatics-2026', 'rosatom', 'informatics', 'информатика', 'informatics', 2, 2026),
	('rosatom-math-2026', 'rosatom', 'math', 'математика', 'math-profile', 2, 2026),
	('rosatom-physics-2026', 'rosatom', 'physics', 'физика', 'physics', 1, 2026),

	('spb-olympiad-math-2026', 'spb-olympiad', 'math', 'математика', 'math-profile', 1, 2026),
	('tournament-of-towns-math-2026', 'tournament-of-towns', 'math', 'математика', 'math-profile', 1, 2026),

	('belchonok-informatics-2026', 'belchonok', 'informatics', 'информатика', 'informatics', 2, 2026),
	('belchonok-math-2026', 'belchonok', 'math', 'математика', 'math-profile', 3, 2026),
	('belchonok-physics-2026', 'belchonok', 'physics', 'физика', 'physics', 3, 2026);

-- ДЕМО-ДАННЫЕ. Вузы и направления совпадают с моками фронта (id, названия, проходные баллы).
-- Правила льгот упрощены по правилам приёма 2026 года с сайтов вузов:
--   МГУ (ВМК):  https://pk.cs.msu.ru/privilege
--   МФТИ:       https://pk.mipt.ru/bachelor/2026_olympiads/
--   НИУ ВШЭ:    https://ba.hse.ru/bolimp, ИД: https://ba.hse.ru/dost
--   ИТМО:       https://abit.itmo.ru/page/113, ИД: https://abit.itmo.ru/page/211
-- В реальности вузы перечисляют конкретные олимпиады по каждому направлению.
-- Здесь правила сведены к «профиль + уровень + степень», чтобы показать механику.

INSERT INTO universities (id, name, short_name, city, achievement_points_max) VALUES
	('msu', 'Московский государственный университет имени М.В. Ломоносова', 'МГУ', 'Москва', 10),
	('hse', 'Национальный исследовательский университет «Высшая школа экономики»', 'НИУ ВШЭ', 'Москва', 10),
	('itmo', 'Университет ИТМО', 'ИТМО', 'Санкт-Петербург', 10),
	('mipt', 'Московский физико-технический институт', 'МФТИ', 'Долгопрудный', 10);

INSERT INTO programs (id, university_id, code, name, passing_score_previous_year) VALUES
	('msu-math', 'msu', '01.03.02', 'Прикладная математика и информатика', 289),
	('msu-informatics', 'msu', '02.03.02', 'Фундаментальная информатика и информационные технологии', 292),
	('hse-software', 'hse', '09.03.04', 'Программная инженерия', 281),
	('hse-data', 'hse', NULL, 'Анализ данных', 287),
	('itmo-software', 'itmo', '09.03.04', 'Программная инженерия', 278),
	('itmo-ai', 'itmo', NULL, 'Искусственный интеллект', 284),
	('itmo-infosec', 'itmo', '10.03.01', 'Информационная безопасность', 270),
	('mipt-applied-math', 'mipt', '01.03.02', 'Прикладная математика и информатика', 296);

INSERT INTO program_subjects (program_id, subject_id, choice_group) VALUES
	('msu-math', 'math-profile', NULL), ('msu-math', 'informatics', NULL), ('msu-math', 'russian', NULL),
	('msu-informatics', 'math-profile', NULL), ('msu-informatics', 'informatics', NULL), ('msu-informatics', 'russian', NULL),
	('hse-software', 'math-profile', NULL), ('hse-software', 'informatics', NULL), ('hse-software', 'russian', NULL),
	('hse-data', 'math-profile', NULL), ('hse-data', 'informatics', NULL), ('hse-data', 'russian', NULL),
	('itmo-software', 'math-profile', NULL), ('itmo-software', 'russian', NULL),
	('itmo-software', 'informatics', 1), ('itmo-software', 'physics', 1),
	('itmo-ai', 'math-profile', NULL), ('itmo-ai', 'russian', NULL),
	('itmo-ai', 'informatics', 1), ('itmo-ai', 'physics', 1),
	('itmo-infosec', 'math-profile', NULL), ('itmo-infosec', 'russian', NULL),
	('itmo-infosec', 'informatics', 1), ('itmo-infosec', 'physics', 1),
	('mipt-applied-math', 'math-profile', NULL), ('mipt-applied-math', 'russian', NULL),
	('mipt-applied-math', 'informatics', 1), ('mipt-applied-math', 'physics', 1);

INSERT INTO program_olympiad_profiles (program_id, profile) VALUES
	('msu-math', 'math'), ('msu-math', 'informatics'),
	('msu-informatics', 'math'), ('msu-informatics', 'informatics'),
	('hse-software', 'math'), ('hse-software', 'informatics'),
	('hse-data', 'math'), ('hse-data', 'informatics'),
	('itmo-software', 'math'), ('itmo-software', 'informatics'), ('itmo-software', 'physics'),
	('itmo-ai', 'math'), ('itmo-ai', 'informatics'), ('itmo-ai', 'physics'), ('itmo-ai', 'ai'),
	('itmo-infosec', 'math'), ('itmo-infosec', 'informatics'), ('itmo-infosec', 'physics'),
	('mipt-applied-math', 'math'), ('mipt-applied-math', 'informatics'), ('mipt-applied-math', 'physics');

INSERT INTO olympiad_benefit_rules
	(university_id, program_id, olympiad_id, profile, vsosh, max_level, degree, benefit, points, min_ege_score) VALUES
	-- ВсОШ: БВИ в любом вузе на направления, соответствующие профилю.
	(NULL, NULL, NULL, 'math', TRUE, NULL, NULL, 'BVI', NULL, NULL),
	(NULL, NULL, NULL, 'informatics', TRUE, NULL, NULL, 'BVI', NULL, NULL),
	(NULL, NULL, NULL, 'physics', TRUE, NULL, NULL, 'BVI', NULL, NULL),

	-- МГУ, ВМК: БВИ только на ПМИ и только победителям I уровня; остальным I–II уровня 100 баллов.
	('msu', 'msu-math', NULL, 'math', FALSE, 1, 'WINNER', 'BVI', NULL, 75),
	('msu', 'msu-math', NULL, 'informatics', FALSE, 1, 'WINNER', 'BVI', NULL, 75),
	('msu', NULL, NULL, 'math', FALSE, 2, NULL, 'SCORE_100', NULL, 75),
	('msu', NULL, NULL, 'informatics', FALSE, 2, NULL, 'SCORE_100', NULL, 75),

	-- НИУ ВШЭ: БВИ победителям, призёрам 100 баллов.
	('hse', NULL, NULL, 'math', FALSE, 2, 'WINNER', 'BVI', NULL, 75),
	('hse', NULL, NULL, 'informatics', FALSE, 2, 'WINNER', 'BVI', NULL, 75),
	('hse', NULL, NULL, 'math', FALSE, 3, NULL, 'SCORE_100', NULL, 75),
	('hse', NULL, NULL, 'informatics', FALSE, 3, NULL, 'SCORE_100', NULL, 75),

	-- ИТМО: БВИ за информатику I–II и математику I уровня, олимпиада по ИИ даёт БВИ на «Искусственный интеллект».
	('itmo', NULL, NULL, 'informatics', FALSE, 2, NULL, 'BVI', NULL, 75),
	('itmo', NULL, NULL, 'math', FALSE, 1, NULL, 'BVI', NULL, 75),
	('itmo', 'itmo-ai', NULL, 'ai', FALSE, 2, NULL, 'BVI', NULL, 75),
	('itmo', NULL, NULL, 'informatics', FALSE, 3, NULL, 'SCORE_100', NULL, 75),
	('itmo', NULL, NULL, 'math', FALSE, 3, NULL, 'SCORE_100', NULL, 75),
	('itmo', NULL, NULL, 'physics', FALSE, 3, NULL, 'SCORE_100', NULL, 75),
	('itmo', 'itmo-ai', NULL, 'ai', FALSE, 3, NULL, 'SCORE_100', NULL, 75),

	-- МФТИ: 100 баллов за I–II уровень при ЕГЭ от 75, БВИ на ПМИ за I уровень только при ЕГЭ от 85.
	-- «Физтех» по математике (II уровень) даёт БВИ отдельным правилом.
	-- Дипломы III уровня идут в индивидуальные достижения.
	('mipt', 'mipt-applied-math', NULL, 'math', FALSE, 1, NULL, 'BVI', NULL, 85),
	('mipt', 'mipt-applied-math', NULL, 'informatics', FALSE, 1, NULL, 'BVI', NULL, 85),
	('mipt', 'mipt-applied-math', NULL, 'physics', FALSE, 1, NULL, 'BVI', NULL, 85),
	('mipt', 'mipt-applied-math', 'phystech', 'math', FALSE, 2, NULL, 'BVI', NULL, 85),
	('mipt', NULL, NULL, 'math', FALSE, 2, NULL, 'SCORE_100', NULL, 75),
	('mipt', NULL, NULL, 'informatics', FALSE, 2, NULL, 'SCORE_100', NULL, 75),
	('mipt', NULL, NULL, 'physics', FALSE, 2, NULL, 'SCORE_100', NULL, 75),
	('mipt', NULL, NULL, 'math', FALSE, 3, NULL, 'ACHIEVEMENT_POINTS', 5, NULL),
	('mipt', NULL, NULL, 'informatics', FALSE, 3, NULL, 'ACHIEVEMENT_POINTS', 5, NULL),
	('mipt', NULL, NULL, 'physics', FALSE, 3, NULL, 'ACHIEVEMENT_POINTS', 5, NULL);

-- Баллы за ИД. У МФТИ в демо ИД из справочника не учитываются.
INSERT INTO achievement_point_rules (university_id, achievement_id, points) VALUES
	('msu', 'medal_gold', 6),
	('msu', 'medal_silver', 6),
	('hse', 'medal_gold', 3),
	('hse', 'medal_silver', 3),
	('hse', 'gto_gold', 2),
	('hse', 'volunteering', 2),
	('itmo', 'gto_gold', 1);

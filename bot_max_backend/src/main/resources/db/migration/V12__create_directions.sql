CREATE TABLE directions (
    id VARCHAR(20) PRIMARY KEY,
    code VARCHAR(10) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL
);

CREATE TABLE direction_interest_categories (
    direction_id VARCHAR(20) NOT NULL REFERENCES directions(id),
    category_id VARCHAR(50) NOT NULL REFERENCES interest_categories(id),
    PRIMARY KEY (direction_id, category_id)
);
CREATE INDEX idx_direction_categories_category ON direction_interest_categories(category_id);

CREATE TABLE user_directions (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    position INTEGER NOT NULL CHECK (position >= 0),
    direction_id VARCHAR(20) NOT NULL REFERENCES directions(id),
    PRIMARY KEY (user_id, position)
);

-- Начальный каталог; не является полным перечнем направлений РФ.
INSERT INTO directions (id, code, name) VALUES
    ('01.03.02', '01.03.02', 'Прикладная математика и информатика'),
    ('02.03.02', '02.03.02', 'Фундаментальная информатика и информационные технологии'),
    ('09.03.01', '09.03.01', 'Информатика и вычислительная техника'),
    ('09.03.02', '09.03.02', 'Информационные системы и технологии'),
    ('09.03.03', '09.03.03', 'Прикладная информатика'),
    ('09.03.04', '09.03.04', 'Программная инженерия'),
    ('10.03.01', '10.03.01', 'Информационная безопасность'),
    ('31.05.01', '31.05.01', 'Лечебное дело'),
    ('31.05.02', '31.05.02', 'Педиатрия'),
    ('31.05.03', '31.05.03', 'Стоматология'),
    ('06.03.01', '06.03.01', 'Биология'),
    ('19.03.01', '19.03.01', 'Биотехнология'),
    ('38.03.01', '38.03.01', 'Экономика'),
    ('38.03.02', '38.03.02', 'Менеджмент'),
    ('15.03.01', '15.03.01', 'Машиностроение'),
    ('15.03.06', '15.03.06', 'Мехатроника и робототехника'),
    ('40.03.01', '40.03.01', 'Юриспруденция'),
    ('45.03.01', '45.03.01', 'Филология'),
    ('45.03.02', '45.03.02', 'Лингвистика'),
    ('54.03.01', '54.03.01', 'Дизайн'),
    ('03.03.01', '03.03.01', 'Прикладные математика и физика'),
    ('03.03.02', '03.03.02', 'Физика');

INSERT INTO direction_interest_categories (direction_id, category_id) VALUES
    ('01.03.02', 'it'), ('02.03.02', 'it'),
    ('09.03.01', 'it'), ('09.03.02', 'it'), ('09.03.03', 'it'), ('09.03.04', 'it'), ('10.03.01', 'it'),
    ('31.05.01', 'medicine'), ('31.05.02', 'medicine'), ('31.05.03', 'medicine'),
    ('06.03.01', 'biology'), ('19.03.01', 'biology'),
    ('38.03.01', 'economics'), ('38.03.02', 'economics'), ('09.03.03', 'economics'),
    ('15.03.01', 'engineering'), ('15.03.06', 'engineering'), ('09.03.01', 'engineering'),
    ('40.03.01', 'law'),
    ('45.03.01', 'languages'), ('45.03.02', 'languages'),
    ('54.03.01', 'design'),
    ('03.03.01', 'physics'), ('03.03.02', 'physics');

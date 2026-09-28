CREATE TABLE interest_categories (
	id VARCHAR(50) PRIMARY KEY,
	name VARCHAR(100) NOT NULL,
	icon VARCHAR(20) NOT NULL
);

CREATE TABLE user_interest_categories (
	user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
	category_id VARCHAR(50) NOT NULL REFERENCES interest_categories(id),
	PRIMARY KEY (user_id, category_id)
);

INSERT INTO interest_categories (id, name, icon) VALUES
	('it', 'IT', '💻'),
	('medicine', 'Медицина', '🩺'),
	('biology', 'Биология', '🧬'),
	('economics', 'Экономика', '📊'),
	('engineering', 'Инженерия', '⚙️'),
	('law', 'Право', '⚖️'),
	('languages', 'Языки', '🌍'),
	('design', 'Дизайн', '🎨'),
	('physics', 'Физика', '⚛️');

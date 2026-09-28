-- Справочники ИД и льгот пока живут в JSON (catalog/*.json),
-- поэтому внешнего ключа на справочник нет — валидация в сервисе.

CREATE TABLE user_achievements (
	user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
	achievement_id VARCHAR(50) NOT NULL,
	PRIMARY KEY (user_id, achievement_id)
);

CREATE TABLE user_privileges (
	user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
	category_id VARCHAR(50) NOT NULL,
	PRIMARY KEY (user_id, category_id)
);

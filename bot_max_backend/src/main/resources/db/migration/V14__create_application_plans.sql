-- План поступления 5×5: до пяти вузов и до пяти программ в каждом.
-- version растёт на 1 при каждом сохранении; PUT с устаревшей версией получает 409.
-- Строки нет — план не сохранялся (для клиента это версия 0).
CREATE TABLE application_plans (
	user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
	version INTEGER NOT NULL CHECK (version > 0),
	bvi_program_id VARCHAR(80) REFERENCES programs(id),
	saved_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Позиции плана. university_position — приоритет вуза, program_position — приоритет программы внутри вуза.
CREATE TABLE application_plan_items (
	user_id UUID NOT NULL REFERENCES application_plans(user_id) ON DELETE CASCADE,
	program_id VARCHAR(80) NOT NULL REFERENCES programs(id),
	university_id VARCHAR(50) NOT NULL REFERENCES universities(id),
	university_position INTEGER NOT NULL CHECK (university_position BETWEEN 0 AND 4),
	program_position INTEGER NOT NULL CHECK (program_position BETWEEN 0 AND 4),
	PRIMARY KEY (user_id, program_id)
);

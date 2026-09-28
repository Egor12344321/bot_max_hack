package com.runiversityadmisson.bot.domain.applicant.model;

/**
 * Что даёт диплом олимпиады на направлении. Порядок констант = выгода: чем выше, тем выгоднее.
 */
public enum OlympiadBenefit {
	/** Зачисление без вступительных испытаний. */
	BVI("bvi"),
	/** 100 баллов по предмету профиля вместо результата ЕГЭ. */
	SCORE_100("score_100"),
	/** Баллы за индивидуальные достижения. */
	ACHIEVEMENT_POINTS("achievement_points"),
	NONE("none");

	private final String code;

	OlympiadBenefit(String code) {
		this.code = code;
	}

	public String getCode() {
		return code;
	}
}

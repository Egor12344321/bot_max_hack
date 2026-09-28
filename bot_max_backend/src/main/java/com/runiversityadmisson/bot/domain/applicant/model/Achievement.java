package com.runiversityadmisson.bot.domain.applicant.model;

/**
 * Индивидуальное достижение (ИД) из справочника.
 * Пока справочник тестовый и читается из classpath:catalog/achievements.json.
 */
public record Achievement(String id, String name, String description) {
}

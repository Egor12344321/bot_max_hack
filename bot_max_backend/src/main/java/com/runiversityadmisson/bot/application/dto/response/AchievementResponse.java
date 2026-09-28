package com.runiversityadmisson.bot.application.dto.response;

/** exclusiveGroup: из достижений одной группы можно выбрать только одно. */
public record AchievementResponse(String id, String name, String description, String exclusiveGroup) {
}

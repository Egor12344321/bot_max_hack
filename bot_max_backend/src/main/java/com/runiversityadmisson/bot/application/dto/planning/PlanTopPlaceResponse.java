package com.runiversityadmisson.bot.application.dto.planning;

/** Место вуза (1–3) в топ-3 выбранного направления. */
public record PlanTopPlaceResponse(String directionId, String directionName, int place) {
}

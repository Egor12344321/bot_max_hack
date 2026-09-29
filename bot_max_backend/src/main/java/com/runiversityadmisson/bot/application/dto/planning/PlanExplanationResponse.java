package com.runiversityadmisson.bot.application.dto.planning;

/** Почему программа в плане. reason: selected_direction, related_direction, auto_fill, retained. */
public record PlanExplanationResponse(String programId, String reason, String message) {
}

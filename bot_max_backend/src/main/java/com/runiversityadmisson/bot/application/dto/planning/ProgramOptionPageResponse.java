package com.runiversityadmisson.bot.application.dto.planning;

import java.util.List;

/** total — число вариантов до пагинации. */
public record ProgramOptionPageResponse(List<ProgramOptionResponse> items, int total) {
}

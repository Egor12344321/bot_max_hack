package com.runiversityadmisson.bot.application.dto.direction;

import java.util.List;

public record StudyDirectionPageResponse(List<StudyDirectionResponse> items, int total) {
}

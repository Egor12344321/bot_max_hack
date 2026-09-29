package com.runiversityadmisson.bot.application.dto.direction;

import java.util.List;

public record DirectionPageResponse(List<DirectionResponse> items, int total) {
}

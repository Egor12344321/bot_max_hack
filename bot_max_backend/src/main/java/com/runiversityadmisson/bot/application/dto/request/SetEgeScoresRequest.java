package com.runiversityadmisson.bot.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record SetEgeScoresRequest(@NotNull List<@Valid EgeScoreInput> scores) {
}

package com.runiversityadmisson.bot.application.dto.benefit;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** Пустой список допустим — у пользователя может не быть ИД. */
public record SetAchievementsRequest(@NotNull List<@NotBlank String> achievementIds) {
}

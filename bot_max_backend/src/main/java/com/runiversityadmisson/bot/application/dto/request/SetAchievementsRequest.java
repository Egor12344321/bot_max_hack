package com.runiversityadmisson.bot.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** Пустой список допустим — у пользователя может не быть ИД. */
public record SetAchievementsRequest(@NotNull List<@NotBlank String> achievementIds) {
}

package com.runiversityadmisson.bot.application.dto.benefit;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** Пустой список [] — у пользователя нет льгот. */
public record SetPrivilegesRequest(@NotNull List<@NotBlank String> categoryIds) {
}

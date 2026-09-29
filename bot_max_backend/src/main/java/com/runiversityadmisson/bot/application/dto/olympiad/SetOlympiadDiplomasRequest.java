package com.runiversityadmisson.bot.application.dto.olympiad;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** Пустой список [] — у пользователя нет дипломов. */
public record SetOlympiadDiplomasRequest(@NotNull List<@Valid OlympiadDiplomaInput> diplomas) {
}

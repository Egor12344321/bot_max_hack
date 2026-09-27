package com.runiversityadmisson.bot.bot.service;

import java.util.List;

public record BotQuestion(String text, List<List<BotButton>> buttons) {
}

package com.runiversityadmisson.bot.bot.service;

import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class EgeScoreParser {

	public Optional<Integer> parse(String text) {
		if (text == null || !text.trim().matches("[0-9]{1,3}")) {
			return Optional.empty();
		}

		try {
			int score = Integer.parseInt(text.trim());
			if (score < 0 || score > 100) {
				return Optional.empty();
			}
			return Optional.of(score);
		} catch (NumberFormatException exception) {
			return Optional.empty();
		}
	}
}

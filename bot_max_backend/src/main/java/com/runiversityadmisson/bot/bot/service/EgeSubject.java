package com.runiversityadmisson.bot.bot.service;

public record EgeSubject(String id, String russianName, String kazakhName, String kyrgyzName) {

	public String name(String language) {
		return switch (language) {
			case "kk" -> kazakhName;
			case "ky" -> kyrgyzName;
			default -> russianName;
		};
	}
}

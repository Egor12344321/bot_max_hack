package com.runiversityadmisson.bot.application.dto.profile;

import java.util.List;

public record CitizenshipResultResponse(String track, String message, List<LinkResponse> links) {

	public record LinkResponse(String label, String url) {
	}
}

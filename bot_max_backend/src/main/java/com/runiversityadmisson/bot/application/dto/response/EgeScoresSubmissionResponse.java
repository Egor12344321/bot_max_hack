package com.runiversityadmisson.bot.application.dto.response;

import java.util.List;

public record EgeScoresSubmissionResponse(List<EgeScoreResponse> scores, boolean allPassed) {
}

package com.runiversityadmisson.bot.application.dto.exam;

import java.util.List;

public record EgeScoresSubmissionResponse(List<EgeScoreResponse> scores, boolean allPassed) {
}

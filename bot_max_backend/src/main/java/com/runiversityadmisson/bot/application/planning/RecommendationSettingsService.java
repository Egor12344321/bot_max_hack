package com.runiversityadmisson.bot.application.planning;

import com.runiversityadmisson.bot.domain.applicant.ports.profile.UserRepository;
import com.runiversityadmisson.bot.presentation.exception.BadRequestException;
import com.runiversityadmisson.bot.presentation.exception.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecommendationSettingsService {
	private final UserRepository users;
	private final CompetitionService competitions;
	public record Settings(Integer maxScoreDeficit, List<Integer> allowedValues, String competitionType,
			List<String> availableCompetitionTypes) {}
	public record Input(Integer maxScoreDeficit, String competitionType) {}

	@Transactional(readOnly = true)
	public Settings get(UUID id) {
		var user = users.findById(id).orElseThrow(() -> new ResourceNotFoundException("Заявка не найдена"));
		var available = competitions.available(user);
		return new Settings(user.getMaxScoreDeficit(), List.of(0, 10, 15, 20), available.getFirst(), available);
	}

	@Transactional
	public Settings save(UUID id, Integer deficit, String competitionType) {
		if (deficit == null || !List.of(0, 10, 15, 20).contains(deficit))
			throw new BadRequestException("Допустимое отставание: 0, 10, 15 или 20 баллов");
		var user = users.findById(id).orElseThrow(() -> new ResourceNotFoundException("Заявка не найдена"));
		if (competitionType != null && !competitions.available(user).contains(competitionType))
			throw new BadRequestException("Выбранный конкурс недоступен по указанным льготам");
		user.setMaxScoreDeficit(deficit);
		user.setPreferredCompetitionType(competitionType);
		return get(id);
	}
}

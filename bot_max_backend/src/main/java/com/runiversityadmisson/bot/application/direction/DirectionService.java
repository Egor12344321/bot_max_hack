package com.runiversityadmisson.bot.application.direction;

import com.runiversityadmisson.bot.application.dto.direction.DirectionSelection;
import com.runiversityadmisson.bot.application.dto.direction.DirectionPageResponse;
import com.runiversityadmisson.bot.application.dto.direction.DirectionResponse;
import com.runiversityadmisson.bot.domain.applicant.model.direction.Direction;
import com.runiversityadmisson.bot.domain.applicant.model.profile.User;
import com.runiversityadmisson.bot.domain.applicant.ports.direction.DirectionRepository;
import com.runiversityadmisson.bot.domain.applicant.ports.direction.InterestCategoryRepository;
import com.runiversityadmisson.bot.domain.applicant.ports.profile.UserRepository;
import com.runiversityadmisson.bot.presentation.exception.BadRequestException;
import com.runiversityadmisson.bot.presentation.exception.ResourceNotFoundException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DirectionService {
	private final DirectionRepository directionRepository;
	private final InterestCategoryRepository categoryRepository;
	private final UserRepository userRepository;

	@Transactional(readOnly = true)
	public DirectionPageResponse search(String categoryId, String query, int offset, int limit) {
		if (offset < 0 || limit < 1 || limit > 100) {
			throw new BadRequestException("offset должен быть >= 0, limit — от 1 до 100");
		}
		if (categoryId != null && !categoryRepository.existsById(categoryId)) {
			throw new BadRequestException("Неизвестная категория интересов");
		}
		String term = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
		List<DirectionResponse> matches = directionRepository.findAll().stream()
				.filter(d -> categoryId == null || d.getInterestCategoryIds().contains(categoryId))
				.filter(d -> d.getCode().contains(term) || d.getName().toLowerCase(Locale.ROOT).contains(term))
				.sorted(Comparator.comparing(Direction::getCode).thenComparing(Direction::getId))
				.map(d -> new DirectionResponse(d.getId(), d.getCode(), d.getName()))
				.toList();
		return new DirectionPageResponse(matches.stream().skip(offset).limit(limit).toList(), matches.size());
	}

	@Transactional(readOnly = true)
	public DirectionSelection getSelection(UUID sessionId) {
		return new DirectionSelection(List.copyOf(getUser(sessionId).getDirectionIds()));
	}

	@Transactional
	public DirectionSelection replaceSelection(UUID sessionId, List<String> ids) {
		User user = getUser(sessionId);
		if (ids == null || ids.stream().anyMatch(id -> id == null || id.isBlank())
				|| new HashSet<>(ids).size() != ids.size()) {
			throw new BadRequestException("Укажите уникальные непустые ID направлений");
		}
		if (directionRepository.findAllById(ids).size() != ids.size()) {
			throw new BadRequestException("Указано неизвестное направление");
		}
		user.getDirectionIds().clear();
		user.getDirectionIds().addAll(ids);
		return new DirectionSelection(List.copyOf(ids));
	}

	private User getUser(UUID sessionId) {
		return userRepository.findById(sessionId)
				.orElseThrow(() -> new ResourceNotFoundException("Заявка не найдена"));
	}
}

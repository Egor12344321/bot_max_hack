package com.runiversityadmisson.bot.application.session;

import com.runiversityadmisson.bot.application.dto.response.EgeScoreResponse;
import com.runiversityadmisson.bot.application.dto.response.SessionDraftResponse;
import com.runiversityadmisson.bot.domain.applicant.model.EgeScore;
import com.runiversityadmisson.bot.domain.applicant.model.Subject;
import com.runiversityadmisson.bot.domain.applicant.model.User;
import com.runiversityadmisson.bot.domain.applicant.ports.EgeScoreRepository;
import com.runiversityadmisson.bot.domain.applicant.ports.SubjectRepository;
import com.runiversityadmisson.bot.domain.applicant.ports.UserRepository;
import com.runiversityadmisson.bot.presentation.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SessionQueryService {

	private final UserRepository userRepository;
	private final EgeScoreRepository egeScoreRepository;
	private final SubjectRepository subjectRepository;

	@Transactional(readOnly = true)
	public SessionDraftResponse getSession(UUID sessionId) {
		User user = userRepository.findById(sessionId)
				.orElseThrow(() -> new ResourceNotFoundException("Заявка не найдена"));
		List<EgeScore> scores = egeScoreRepository.findByUserId(sessionId);
		Map<String, Subject> subjects = subjectRepository.findAllById(scores.stream()
				.map(EgeScore::getSubjectId)
				.toList())
				.stream()
				.collect(Collectors.toMap(Subject::getId, Function.identity()));

		return new SessionDraftResponse(
				user.getId(),
				user.getLanguage(),
				user.getCitizenship(),
				scores.stream().map(score -> toResponse(score, subjects.get(score.getSubjectId()), user.getLanguage())).toList(),
				true
		);
	}

	private EgeScoreResponse toResponse(EgeScore score, Subject subject, String language) {
		if (subject == null) {
			return new EgeScoreResponse(score.getSubjectId(), score.getSubjectId(), score.getScore(), 0, false);
		}

		String name = switch (language) {
			case "kk" -> subject.getNameKk();
			case "ky" -> subject.getNameKy();
			default -> subject.getNameRu();
		};
		if (name == null || name.isBlank()) {
			name = subject.getNameRu();
		}
		return new EgeScoreResponse(score.getSubjectId(), name, score.getScore(), subject.getMinThreshold(),
				score.getScore() >= subject.getMinThreshold());
	}
}

package com.runiversityadmisson.bot.application.olympiad;

import com.runiversityadmisson.bot.application.dto.olympiad.OlympiadDiplomaInput;
import com.runiversityadmisson.bot.application.dto.olympiad.OlympiadDiplomaResponse;
import com.runiversityadmisson.bot.application.dto.olympiad.OlympiadProfileResponse;
import com.runiversityadmisson.bot.application.dto.olympiad.OlympiadResponse;
import com.runiversityadmisson.bot.domain.applicant.model.olympiad.Olympiad;
import com.runiversityadmisson.bot.domain.applicant.model.olympiad.OlympiadDegree;
import com.runiversityadmisson.bot.domain.applicant.model.olympiad.OlympiadDiploma;
import com.runiversityadmisson.bot.domain.applicant.model.olympiad.OlympiadProfile;
import com.runiversityadmisson.bot.domain.applicant.model.profile.User;
import com.runiversityadmisson.bot.domain.applicant.ports.olympiad.OlympiadProfileRepository;
import com.runiversityadmisson.bot.domain.applicant.ports.olympiad.OlympiadRepository;
import com.runiversityadmisson.bot.domain.applicant.ports.profile.UserRepository;
import com.runiversityadmisson.bot.presentation.exception.BadRequestException;
import com.runiversityadmisson.bot.presentation.exception.ResourceNotFoundException;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Каталог олимпиад и дипломы пользователя. */
@Service
@RequiredArgsConstructor
public class OlympiadService {

	private final OlympiadRepository olympiadRepository;
	private final OlympiadProfileRepository olympiadProfileRepository;
	private final UserRepository userRepository;

	/**
	 * @param query     часть названия олимпиады, без учёта регистра и разницы «е»/«ё»
	 * @param subjectId оставить только профили с этим предметом ЕГЭ
	 */
	@Transactional(readOnly = true)
	public List<OlympiadResponse> getOlympiads(String query, String subjectId) {
		String needle = query == null ? "" : normalize(query.strip());
		return olympiadRepository.findAll().stream()
				.filter(olympiad -> normalize(olympiad.getName()).contains(needle))
				.sorted(Comparator.comparing(Olympiad::isVsosh).reversed().thenComparing(Olympiad::getName))
				.map(olympiad -> toResponse(olympiad, subjectId))
				.filter(olympiad -> !olympiad.profiles().isEmpty())
				.toList();
	}

	@Transactional(readOnly = true)
	public List<OlympiadDiplomaResponse> getDiplomas(UUID sessionId) {
		return toResponses(getUser(sessionId).getOlympiadDiplomas());
	}

	@Transactional
	public List<OlympiadDiplomaResponse> setDiplomas(UUID sessionId, List<OlympiadDiplomaInput> inputs) {
		List<String> profileIds = inputs.stream().map(OlympiadDiplomaInput::profileId).toList();
		if (new LinkedHashSet<>(profileIds).size() != profileIds.size()) {
			throw new BadRequestException("Дипломы не должны повторяться");
		}
		if (olympiadProfileRepository.findAllById(profileIds).size() != profileIds.size()) {
			throw new BadRequestException("Указан неизвестный профиль олимпиады");
		}
		User user = getUser(sessionId);
		user.getOlympiadDiplomas().clear();
		inputs.forEach(input -> user.getOlympiadDiplomas()
				.add(new OlympiadDiploma(input.profileId(), OlympiadDegree.fromCode(input.degree()))));
		return toResponses(user.getOlympiadDiplomas());
	}

	private List<OlympiadDiplomaResponse> toResponses(Collection<OlympiadDiploma> diplomas) {
		Map<String, OlympiadProfile> profiles = olympiadProfileRepository
				.findAllById(diplomas.stream().map(OlympiadDiploma::getProfileId).toList())
				.stream()
				.collect(Collectors.toMap(OlympiadProfile::getId, Function.identity()));
		return diplomas.stream()
				.map(diploma -> {
					OlympiadProfile profile = profiles.get(diploma.getProfileId());
					return new OlympiadDiplomaResponse(profile.getId(), profile.getOlympiad().getId(),
							profile.getOlympiad().getName(), profile.getName(), profile.getLevel(),
							diploma.getDegree().getCode(), profile.getOlympiadYear());
				})
				.toList();
	}

	private static OlympiadResponse toResponse(Olympiad olympiad, String subjectId) {
		List<OlympiadProfileResponse> profiles = olympiad.getProfiles().stream()
				.filter(profile -> subjectId == null || subjectId.equals(profile.getSubjectId()))
				.map(profile -> new OlympiadProfileResponse(profile.getId(), profile.getProfile(), profile.getName(),
						profile.getSubjectId(), profile.getLevel(), profile.getOlympiadYear()))
				.toList();
		return new OlympiadResponse(olympiad.getId(), olympiad.getName(), olympiad.isVsosh(),
				olympiad.getListNumber(), profiles);
	}

	private static String normalize(String text) {
		return text.toLowerCase(Locale.ROOT).replace('ё', 'е');
	}

	private User getUser(UUID sessionId) {
		return userRepository.findById(sessionId)
				.orElseThrow(() -> new ResourceNotFoundException("Заявка не найдена"));
	}
}

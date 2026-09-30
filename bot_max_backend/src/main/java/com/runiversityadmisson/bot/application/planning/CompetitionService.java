package com.runiversityadmisson.bot.application.planning;

import com.runiversityadmisson.bot.application.benefit.PrivilegeCatalogService;
import com.runiversityadmisson.bot.domain.applicant.model.profile.User;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CompetitionService {
	private final PrivilegeCatalogService privileges;

	public List<String> available(User user) {
		List<String> result = new ArrayList<>();
		for (String type : List.of("special_quota", "separate_quota", "target_quota")) {
			if (user.getPrivilegeCategoryIds().stream().anyMatch(id -> privileges.findPrivilegeCategory(id)
					.map(category -> type.equals(category.quotaType().getCode())).orElse(false))) result.add(type);
		}
		result.add("general");
		if (user.getPreferredCompetitionType() != null && result.remove(user.getPreferredCompetitionType())) {
			result.addFirst(user.getPreferredCompetitionType());
		}
		return List.copyOf(result);
	}
}

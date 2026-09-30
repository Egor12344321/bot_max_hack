package com.runiversityadmisson.bot;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.runiversityadmisson.bot.application.benefit.AchievementPrivilegeService;
import com.runiversityadmisson.bot.application.direction.StudyDirectionService;
import com.runiversityadmisson.bot.application.dto.exam.EgeScoreInput;
import com.runiversityadmisson.bot.application.onboarding.QuestionnaireService;
import com.runiversityadmisson.bot.domain.applicant.model.profile.User;
import com.runiversityadmisson.bot.domain.applicant.ports.profile.UserRepository;
import com.runiversityadmisson.bot.presentation.security.JwtService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/** План 5×5 по HTTP на данных V13: «Программная инженерия», ЕГЭ 80/85/90 и медаль (итого 260). */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApplicationPlanIntegrationTests {

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private JwtService jwtService;
	@Autowired
	private QuestionnaireService questionnaireService;
	@Autowired
	private StudyDirectionService directionService;
	@Autowired
	private AchievementPrivilegeService achievementPrivilegeService;

	private UUID userId;
	private String bearer;

	@BeforeEach
	void createUser() {
		User user = new User();
		user.setMaxUserId(System.nanoTime());
		user.setLanguage("ru");
		userId = userRepository.save(user).getId();
		bearer = "Bearer " + jwtService.generate(userId);
		directionService.replaceSelection(userId, List.of("09.03.04"));
		questionnaireService.setEgeScores(userId, List.of(
				new EgeScoreInput("math-profile", 80),
				new EgeScoreInput("informatics", 85),
				new EgeScoreInput("russian", 90)));
		achievementPrivilegeService.setAchievements(userId, List.of("medal_gold"));
	}

	@Test
	void newUserHasEmptyPlanWithVersionZero() throws Exception {
		mockMvc.perform(authorized(get("/v1/sessions/{id}/application-plan", userId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.version").value(0))
				.andExpect(jsonPath("$.composition.universities").isEmpty())
				.andExpect(jsonPath("$.savedAt").isEmpty());
	}

	@Test
	void generatesPlanFromTopThreeAndFill() throws Exception {
		// Допустимое отставание 15: проходят ФУ (270), МИЭТ (270), СТАНКИН (262), МИРЭА (246), РГСУ (241), РХТУ (215)
		// и МИФИ без проходного. По проходному: ФУ и МИЭТ, затем СТАНКИН — это топ-3; МИРЭА и РГСУ — добор.
		mockMvc.perform(authorized(post("/v1/sessions/{id}/application-plan/preview", userId))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"mode\":\"generate\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.composition.universities[*].universityId")
						.value(org.hamcrest.Matchers.contains("fa", "miet", "stankin", "mirea", "rgsu")))
				.andExpect(jsonPath("$.composition.bviProgramId").isEmpty())
				.andExpect(jsonPath("$.composition.universities[0].programIds[0]").value("fa-090304"))
				.andExpect(jsonPath("$.explanations[0].reason").value("selected_direction"))
				.andExpect(jsonPath("$.explanations[1].reason").value("related_direction"))
				.andExpect(jsonPath("$.universityRanking[0].places[0].place").value(1))
				.andExpect(jsonPath("$.universityRanking[3].addedAsFill").value(true))
				.andExpect(jsonPath("$.nearPreviousThreshold").value(5));

		// Предпросмотр ничего не сохраняет.
		mockMvc.perform(authorized(get("/v1/sessions/{id}/application-plan", userId)))
				.andExpect(jsonPath("$.version").value(0));
	}

	@Test
	void strictDeficitShrinksPlan() throws Exception {
		mockMvc.perform(authorized(post("/v1/sessions/{id}/application-plan/preview", userId))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"mode\":\"generate\",\"allowedDeficit\":0}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.composition.universities[*].universityId")
						.value(org.hamcrest.Matchers.contains("mirea", "rgsu", "muctr", "mephi")))
				.andExpect(jsonPath("$.warnings[0]").value("Подходящих вузов: 4 из 5. Неподходящие варианты автоплан не добавляет."));
	}

	@Test
	void fillKeepsUserChoicesFirst() throws Exception {
		mockMvc.perform(authorized(post("/v1/sessions/{id}/application-plan/preview", userId))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"mode\":\"fill\",\"basePlan\":{\"universities\":[{\"universityId\":\"rgsu\","
								+ "\"programIds\":[\"rgsu-090304\"]}],\"bviProgramId\":null}}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.composition.universities[*].universityId")
						.value(org.hamcrest.Matchers.contains("rgsu", "fa", "miet", "stankin", "mirea")))
				.andExpect(jsonPath("$.explanations[0].reason").value("retained"))
				.andExpect(jsonPath("$.universityRanking[0].retained").value(true));
	}

	@Test
	void rejectsWrongPreviewRequests() throws Exception {
		mockMvc.perform(authorized(post("/v1/sessions/{id}/application-plan/preview", userId))
						.contentType(MediaType.APPLICATION_JSON).content("{\"mode\":\"fill\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Для режима fill нужен basePlan"));
		mockMvc.perform(authorized(post("/v1/sessions/{id}/application-plan/preview", userId))
						.contentType(MediaType.APPLICATION_JSON).content("{\"mode\":\"generate\",\"allowedDeficit\":7}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void savesWithVersionsAndRejectsStaleVersion() throws Exception {
		String body = "{\"universities\":[{\"universityId\":\"stankin\",\"programIds\":[\"stankin-090304\"]}],"
				+ "\"bviProgramId\":null,\"expectedVersion\":%d}";
		mockMvc.perform(authorized(put("/v1/sessions/{id}/application-plan", userId))
						.contentType(MediaType.APPLICATION_JSON).content(body.formatted(0)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.version").value(1))
				.andExpect(jsonPath("$.options[0].programId").value("stankin-090304"));

		mockMvc.perform(authorized(put("/v1/sessions/{id}/application-plan", userId))
						.contentType(MediaType.APPLICATION_JSON).content(body.formatted(0)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("version_conflict"));

		mockMvc.perform(authorized(get("/v1/sessions/{id}/application-plan", userId)))
				.andExpect(jsonPath("$.version").value(1))
				.andExpect(jsonPath("$.composition.universities[0].programIds[0]").value("stankin-090304"));
	}

	@Test
	void rejectsInvalidPlans() throws Exception {
		assertBadRequest("[{\"universityId\":\"stankin\",\"programIds\":[\"stankin-090304\",\"stankin-090304\"]}]", null,
				"Программа «Программная инженерия (Системный анализ и проектирование программных комплексов)» указана дважды");
		assertBadRequest("[{\"universityId\":\"stankin\",\"programIds\":[\"fa-090304\"]}]", null,
				"Программа «Программная инженерия (Разработка и внедрение информационно-аналитических систем)» не относится к вузу «stankin»");
		// Нет ЕГЭ по физике: по направлению «Физика» допуска нет.
		assertBadRequest("[{\"universityId\":\"hse\",\"programIds\":[\"hse-030302\"]}]", null,
				"«Физика» (НИУ ВШЭ): нет допуска к конкурсу, не хватает ЕГЭ или балл ниже минимального");
		assertBadRequest("[{\"universityId\":\"stankin\",\"programIds\":[\"stankin-090304\"]}]", "\"fa-090304\"",
				"Программа, где используется БВИ, должна быть в плане");
	}

	@Test
	void manuallyAddsReordersAndRemovesUniversitiesFromSelectedDirections() throws Exception {
		mockMvc.perform(authorized(get("/v1/sessions/{id}/application-plan/options", userId).param("universityId", "stankin")))
				.andExpect(status().isOk()).andExpect(jsonPath("$.items[0].programId").value("stankin-090304"))
				.andExpect(jsonPath("$.total").value(1));
		String stankin = "{\"universityId\":\"stankin\",\"programIds\":[\"stankin-090304\"]}";
		String fa = "{\"universityId\":\"fa\",\"programIds\":[\"fa-090304\"]}";
		String body = "{\"expectedVersion\":%d,\"universities\":[%s]}";
		mockMvc.perform(authorized(put("/v1/sessions/{id}/application-plan", userId)).contentType(MediaType.APPLICATION_JSON)
				.content(body.formatted(0, stankin + "," + fa))).andExpect(status().isOk());
		mockMvc.perform(authorized(put("/v1/sessions/{id}/application-plan", userId)).contentType(MediaType.APPLICATION_JSON)
				.content(body.formatted(1, fa + "," + stankin))).andExpect(status().isOk())
				.andExpect(jsonPath("$.composition.universities[0].universityId").value("fa"));
		mockMvc.perform(authorized(put("/v1/sessions/{id}/application-plan", userId)).contentType(MediaType.APPLICATION_JSON)
				.content(body.formatted(2, fa))).andExpect(status().isOk())
				.andExpect(jsonPath("$.composition.universities.length()").value(1))
				.andExpect(jsonPath("$.version").value(3));
	}

	private void assertBadRequest(String universities, String bvi, String message) throws Exception {
		mockMvc.perform(authorized(put("/v1/sessions/{id}/application-plan", userId))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"universities\":" + universities + ",\"bviProgramId\":" + bvi
								+ ",\"expectedVersion\":0}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(message));
	}

	private MockHttpServletRequestBuilder authorized(MockHttpServletRequestBuilder request) {
		return request.header(HttpHeaders.AUTHORIZATION, bearer);
	}
}

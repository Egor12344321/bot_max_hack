package com.runiversityadmisson.bot;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.runiversityadmisson.bot.domain.applicant.model.profile.User;
import com.runiversityadmisson.bot.domain.applicant.ports.profile.UserRepository;
import com.runiversityadmisson.bot.presentation.security.JwtService;
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
import org.springframework.transaction.annotation.Transactional;

/** HTTP-уровень: авторизация, профиль и ошибки на русском. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiHttpIntegrationTests {

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private JwtService jwtService;

	private UUID userId;
	private String bearer;

	@BeforeEach
	void createUser() {
		User user = new User();
		user.setMaxUserId(System.nanoTime());
		user.setLanguage("ru");
		user.setCitizenship("RU");
		user.setTrack("domestic_equivalent");
		userId = userRepository.save(user).getId();
		bearer = "Bearer " + jwtService.generate(userId);
	}

	@Test
	void returnsProfile() throws Exception {
		mockMvc.perform(get("/v1/sessions/{id}/profile", userId).header(HttpHeaders.AUTHORIZATION, bearer))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.egeTotal").value(0))
				.andExpect(jsonPath("$.programs.total").value(0))
				.andExpect(jsonPath("$.advice").value("Добавь баллы ЕГЭ: без них подборку не посчитать."));
	}

	@Test
	void skipsOptionalOnboardingWithoutErasingSelection() throws Exception {
		mockMvc.perform(put("/v1/sessions/{id}/directions", userId).header(HttpHeaders.AUTHORIZATION, bearer)
				.contentType(MediaType.APPLICATION_JSON).content("{\"directionIds\":[\"09.03.04\"]}"))
				.andExpect(status().isOk());
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
				"/v1/sessions/{id}/onboarding/complete", userId).header(HttpHeaders.AUTHORIZATION, bearer))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.miniAppOnboardingComplete").value(true))
				.andExpect(jsonPath("$.directionIds[0]").value("09.03.04"));
		mockMvc.perform(get("/v1/sessions/{id}", userId).header(HttpHeaders.AUTHORIZATION, bearer))
				.andExpect(jsonPath("$.miniAppOnboardingComplete").value(true));
	}

	@Test
	void settingsValidateDeficitAndQuotaAndOwnership() throws Exception {
		mockMvc.perform(put("/v1/sessions/{id}/recommendation-settings", userId).header(HttpHeaders.AUTHORIZATION, bearer)
				.contentType(MediaType.APPLICATION_JSON).content("{\"maxScoreDeficit\":0}"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.maxScoreDeficit").value(0));
		mockMvc.perform(put("/v1/sessions/{id}/recommendation-settings", userId).header(HttpHeaders.AUTHORIZATION, bearer)
				.contentType(MediaType.APPLICATION_JSON).content("{\"maxScoreDeficit\":7}"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(put("/v1/sessions/{id}/recommendation-settings", userId).header(HttpHeaders.AUTHORIZATION, bearer)
				.contentType(MediaType.APPLICATION_JSON).content("{\"maxScoreDeficit\":15,\"competitionType\":\"separate_quota\"}"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(get("/v1/sessions/{id}/recommendation-settings", UUID.randomUUID())
				.header(HttpHeaders.AUTHORIZATION, bearer)).andExpect(status().isNotFound());
	}

	@Test
	void validationErrorsAreInRussian() throws Exception {
		mockMvc.perform(put("/v1/sessions/{id}/ege-scores", userId)
						.header(HttpHeaders.AUTHORIZATION, bearer)
						.header(HttpHeaders.ACCEPT_LANGUAGE, "en")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"scores\":[{\"subjectId\":\"physics\",\"score\":150}]}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("validation_failed"))
				.andExpect(jsonPath("$.message").value("Некорректное поле «scores[0].score»: должно быть не больше 100"));
	}

	@Test
	void unknownPathIsNotFoundInRussian() throws Exception {
		mockMvc.perform(get("/v1/sessions/{id}/strategy/report", userId).header(HttpHeaders.AUTHORIZATION, bearer))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Этот раздел пока недоступен на сервере"));
	}

	@Test
	void missingParameterIsBadRequest() throws Exception {
		mockMvc.perform(get("/v1/sessions/{id}/recommendations", userId)
						.header(HttpHeaders.AUTHORIZATION, bearer)
						.param("offset", "abc"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Некорректные параметры запроса"));
	}
}

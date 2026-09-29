package com.runiversityadmisson.bot.application.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.runiversityadmisson.bot.application.dto.auth.ExchangeResponse;
import com.runiversityadmisson.bot.domain.applicant.model.profile.User;
import com.runiversityadmisson.bot.domain.applicant.ports.profile.UserRepository;
import com.runiversityadmisson.bot.presentation.exception.ResourceNotFoundException;
import com.runiversityadmisson.bot.presentation.security.InitDataValidator;
import com.runiversityadmisson.bot.presentation.security.JwtService;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuthTokenServiceTest {

	@Test
	void returnsPublicSessionAndUsesUserUuidForJwt() {
		InitDataValidator initDataValidator = mock(InitDataValidator.class);
		JwtService jwtService = mock(JwtService.class);
		UserRepository userRepository = mock(UserRepository.class);
		User user = new User();
		UUID userId = UUID.randomUUID();
		LocalDateTime createdAt = LocalDateTime.of(2026, 9, 28, 12, 0);
		user.setId(userId);
		user.setMaxUserId(42L);
		user.setLanguage("kk");
		user.setCitizenship("KZ");
		user.setCreatedAt(createdAt);

		when(initDataValidator.validateAndExtractUserId("init-data")).thenReturn(42L);
		when(userRepository.findByMaxUserId(42L)).thenReturn(Optional.of(user));
		when(jwtService.generate(userId)).thenReturn("token");
		when(jwtService.getTtlSeconds()).thenReturn(7200L);

		ExchangeResponse response = new AuthTokenService(initDataValidator, jwtService, userRepository)
				.exchange("init-data");

		assertThat(response.accessToken()).isEqualTo("token");
		assertThat(response.expiresIn()).isEqualTo(7200);
		assertThat(response.session().id()).isEqualTo(userId);
		assertThat(response.session().platform()).isEqualTo("max");
		assertThat(response.session().countryCode()).isEqualTo("KZ");
		assertThat(response.session().createdAt()).isEqualTo(createdAt);
		verify(jwtService).generate(userId);
	}

	@Test
	void doesNotCreateUserForDirectMiniAppOpen() {
		InitDataValidator initDataValidator = mock(InitDataValidator.class);
		JwtService jwtService = mock(JwtService.class);
		UserRepository userRepository = mock(UserRepository.class);
		when(initDataValidator.validateAndExtractUserId("init-data")).thenReturn(42L);
		when(userRepository.findByMaxUserId(42L)).thenReturn(Optional.empty());

		AuthTokenService exchangeService = new AuthTokenService(initDataValidator, jwtService, userRepository);

		assertThatThrownBy(() -> exchangeService.exchange("init-data"))
				.isInstanceOf(ResourceNotFoundException.class);
	}
}

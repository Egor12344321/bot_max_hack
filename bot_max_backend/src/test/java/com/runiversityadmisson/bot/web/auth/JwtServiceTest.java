package com.runiversityadmisson.bot.web.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

	@Test
	void generatesTokenWithUserUuidAsSubject() {
		JwtService jwtService = new JwtService("test-secret-must-have-at-least-32-characters", 3600);
		UUID userId = UUID.randomUUID();

		String token = jwtService.generate(userId);

		assertThat(jwtService.extractUserId(token)).isEqualTo(userId);
	}
}

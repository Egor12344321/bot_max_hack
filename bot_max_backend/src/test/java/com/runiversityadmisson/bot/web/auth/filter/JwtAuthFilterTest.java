package com.runiversityadmisson.bot.web.auth.filter;

import static org.assertj.core.api.Assertions.assertThat;

import com.runiversityadmisson.bot.web.auth.JwtService;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthFilterTest {

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void authenticatesUserByUuidFromBearerToken() throws Exception {
		JwtService jwtService = new JwtService("test-secret-must-have-at-least-32-characters", 3600);
		UUID userId = UUID.randomUUID();
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer " + jwtService.generate(userId));
		AtomicReference<Object> principal = new AtomicReference<>();

		new JwtAuthFilter(jwtService).doFilter(request, new MockHttpServletResponse(), (ignoredRequest, ignoredResponse) ->
				principal.set(SecurityContextHolder.getContext().getAuthentication().getPrincipal()));

		assertThat(principal.get()).isEqualTo(userId);
	}

	@Test
	void rejectsInvalidBearerToken() throws Exception {
		JwtService jwtService = new JwtService("test-secret-must-have-at-least-32-characters", 3600);
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.addHeader("Authorization", "Bearer invalid");

		new JwtAuthFilter(jwtService).doFilter(request, response, (ignoredRequest, ignoredResponse) -> {
			throw new AssertionError("Filter chain must not run");
		});

		assertThat(response.getStatus()).isEqualTo(401);
	}
}

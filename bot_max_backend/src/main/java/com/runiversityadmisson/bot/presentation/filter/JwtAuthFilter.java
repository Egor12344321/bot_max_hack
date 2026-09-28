package com.runiversityadmisson.bot.presentation.filter;

import com.runiversityadmisson.bot.presentation.exception.InvalidJwtException;
import com.runiversityadmisson.bot.presentation.security.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		boolean publicPost = HttpMethod.POST.matches(request.getMethod())
				&& ("/v1/auth/exchange".equals(request.getRequestURI())
				|| "/webhook/max".equals(request.getRequestURI()));
		if (publicPost) {
			log.debug("JWT filter: публичный маршрут пропущен: {} {}",
					request.getMethod(), request.getRequestURI());
		}
		return publicPost;
	}

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
			log.debug("JWT filter: Bearer token отсутствует, запрос передан дальше: {} {}",
					request.getMethod(), request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = authorization.substring("Bearer ".length());
			UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    jwtService.extractUserId(token), null, List.of());
            SecurityContextHolder.getContext().setAuthentication(authentication);
			log.debug("JWT filter: Bearer token принят: {} {}", request.getMethod(), request.getRequestURI());
            filterChain.doFilter(request, response);
        } catch (InvalidJwtException exception) {
            SecurityContextHolder.clearContext();
			log.warn("JWT filter: Bearer token отклонён: {} {}, причина={}",
					request.getMethod(), request.getRequestURI(), exception.getClass().getSimpleName());
            response.sendError(HttpStatus.UNAUTHORIZED.value());
        }
    }
}

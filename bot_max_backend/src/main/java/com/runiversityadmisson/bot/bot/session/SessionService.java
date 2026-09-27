package com.runiversityadmisson.bot.bot.session;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class SessionService {

	private static final String PREFIX = "abiturient:session:";

	private final RedisTemplate<String, Session> redisTemplate;
	private final Duration sessionTtl;

	public SessionService(
			RedisTemplate<String, Session> redisTemplate,
			@Value("${bot.session-ttl:PT1H}") Duration sessionTtl
	) {
		this.redisTemplate = redisTemplate;
		this.sessionTtl = sessionTtl;
	}

	public Session getOrCreate(Long userId) {
		Session session = redisTemplate.opsForValue().get(PREFIX + userId);
		if (session == null) {
			session = new Session();
			session.setUserId(userId);
			save(session);
		}
		return session;
	}

	public Session findByUserId(Long userId) {
		return redisTemplate.opsForValue().get(PREFIX + userId);
	}

	public void save(Session session) {
		redisTemplate.opsForValue().set(PREFIX + session.getUserId(), session, sessionTtl);
	}

	public void delete(Long userId) {
		redisTemplate.delete(PREFIX + userId);
	}
}

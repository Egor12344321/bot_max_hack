package com.runiversityadmisson.bot.bot.session;

import java.time.Duration;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SessionService {

	private static final String KEY_PREFIX = "session:";

	private final RedisTemplate<String, BotSession> botSessionRedisTemplate;

	@Value("${bot.session-ttl:PT1H}")
	private Duration sessionTtl;

	public Optional<BotSession> get(long userId) {
		return Optional.ofNullable(botSessionRedisTemplate.opsForValue().get(key(userId)));
	}

	public BotSession getOrCreate(long userId) {
		return get(userId).orElseGet(() -> {
			BotSession session = BotSession.initial(userId);
			save(session);
			return session;
		});
	}

	public void save(BotSession session) {
		botSessionRedisTemplate.opsForValue().set(key(session.userId()), session, sessionTtl);
	}

	public void clear(long userId) {
		botSessionRedisTemplate.delete(key(userId));
	}

	private String key(long userId) {
		return KEY_PREFIX + userId;
	}
}

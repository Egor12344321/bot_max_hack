package com.runiversityadmisson.bot.application.max.dialog;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class BotQuestionnaireStore {

	private static final String PREFIX = "abiturient:session:";

	private final RedisTemplate<String, BotQuestionnaire> redisTemplate;
	private final Duration sessionTtl;

	public BotQuestionnaireStore(
			RedisTemplate<String, BotQuestionnaire> redisTemplate,
			@Value("${bot.session-ttl:PT1H}") Duration sessionTtl
	) {
		this.redisTemplate = redisTemplate;
		this.sessionTtl = sessionTtl;
	}

	public BotQuestionnaire getOrCreate(Long userId) {
		BotQuestionnaire session = redisTemplate.opsForValue().get(PREFIX + userId);
		if (session == null) {
			session = new BotQuestionnaire();
			session.setUserId(userId);
			save(session);
		}
		return session;
	}

	public BotQuestionnaire findByUserId(Long userId) {
		return redisTemplate.opsForValue().get(PREFIX + userId);
	}

	public void save(BotQuestionnaire session) {
		redisTemplate.opsForValue().set(PREFIX + session.getUserId(), session, sessionTtl);
	}

	public void delete(Long userId) {
		redisTemplate.delete(PREFIX + userId);
	}
}

package com.runiversityadmisson.bot.config;

import com.runiversityadmisson.bot.bot.session.Session;
import com.runiversityadmisson.bot.bot.session.SessionState;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RedisConfigTest {

	private final RedisTemplate<String, Session> redisTemplate = new RedisConfig().redisTemplate(
			mock(RedisConnectionFactory.class),
			JsonMapper.builder().build()
	);

	@Test
	void sessionRoundTripsThroughConfiguredSerializer() {
		RedisSerializer<Session> serializer = valueSerializer();

		Session original = new Session();
		original.setUserId(4242L);
		original.setLanguage("kk");
		original.setCitizenship("KZ");
		original.setTrack("domestic_equivalent");
		original.setState(SessionState.WAITING_FOR_EGE_SCORE);
		original.setCurrentSubject("math-profile");
		original.getEgeScores().put("math-profile", 80);

		Session restored = serializer.deserialize(serializer.serialize(original));

		assertThat(restored).isNotNull();
		assertThat(restored.getUserId()).isEqualTo(4242L);
		assertThat(restored.getLanguage()).isEqualTo("kk");
		assertThat(restored.getCitizenship()).isEqualTo("KZ");
		assertThat(restored.getTrack()).isEqualTo("domestic_equivalent");
		assertThat(restored.getState()).isEqualTo(SessionState.WAITING_FOR_EGE_SCORE);
		assertThat(restored.getCurrentSubject()).isEqualTo("math-profile");
		assertThat(restored.getEgeScores()).containsEntry("math-profile", 80);
	}

	@Test
	void keysAreStoredAsStrings() {
		RedisSerializer<String> keySerializer = keySerializer();
		assertThat(keySerializer.serialize("abiturient:session:1"))
				.isEqualTo("abiturient:session:1".getBytes(StandardCharsets.UTF_8));
	}

	@SuppressWarnings("unchecked")
	private RedisSerializer<Session> valueSerializer() {
		return (RedisSerializer<Session>) redisTemplate.getValueSerializer();
	}

	@SuppressWarnings("unchecked")
	private RedisSerializer<String> keySerializer() {
		return (RedisSerializer<String>) redisTemplate.getKeySerializer();
	}
}

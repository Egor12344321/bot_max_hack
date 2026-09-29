package com.runiversityadmisson.bot.infrastructure.config;

import com.runiversityadmisson.bot.application.max.dialog.BotQuestionnaire;
import com.runiversityadmisson.bot.application.max.dialog.BotQuestionnaireStep;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RedisConfigTest {

	private final RedisTemplate<String, BotQuestionnaire> redisTemplate = new RedisConfig().redisTemplate(
			mock(RedisConnectionFactory.class),
			JsonMapper.builder().build()
	);

	@Test
	void sessionRoundTripsThroughConfiguredSerializer() {
		RedisSerializer<BotQuestionnaire> serializer = valueSerializer();

		BotQuestionnaire original = new BotQuestionnaire();
		original.setUserId(4242L);
		original.setLanguage("kk");
		original.setCitizenship("KZ");
		original.setTrack("domestic_equivalent");
		original.setState(BotQuestionnaireStep.WAITING_FOR_EGE_SCORE);
		original.setCurrentSubject("math-profile");
		original.getEgeScores().put("math-profile", 80);

		BotQuestionnaire restored = serializer.deserialize(serializer.serialize(original));

		assertThat(restored).isNotNull();
		assertThat(restored.getUserId()).isEqualTo(4242L);
		assertThat(restored.getLanguage()).isEqualTo("kk");
		assertThat(restored.getCitizenship()).isEqualTo("KZ");
		assertThat(restored.getTrack()).isEqualTo("domestic_equivalent");
		assertThat(restored.getState()).isEqualTo(BotQuestionnaireStep.WAITING_FOR_EGE_SCORE);
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
	private RedisSerializer<BotQuestionnaire> valueSerializer() {
		return (RedisSerializer<BotQuestionnaire>) redisTemplate.getValueSerializer();
	}

	@SuppressWarnings("unchecked")
	private RedisSerializer<String> keySerializer() {
		return (RedisSerializer<String>) redisTemplate.getKeySerializer();
	}
}

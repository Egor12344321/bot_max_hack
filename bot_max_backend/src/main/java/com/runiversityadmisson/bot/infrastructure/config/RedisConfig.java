package com.runiversityadmisson.bot.infrastructure.config;

import com.runiversityadmisson.bot.application.max.dialog.BotQuestionnaire;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class RedisConfig {

	@Bean
	public RedisTemplate<String, BotQuestionnaire> redisTemplate(
			RedisConnectionFactory connectionFactory,
			ObjectMapper objectMapper
	) {
		RedisTemplate<String, BotQuestionnaire> template = new RedisTemplate<>();
		template.setConnectionFactory(connectionFactory);
		template.setKeySerializer(new StringRedisSerializer());
		template.setValueSerializer(new JacksonJsonRedisSerializer<>(objectMapper, BotQuestionnaire.class));
		template.afterPropertiesSet();
		return template;
	}
}

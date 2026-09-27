package com.runiversityadmisson.bot.config;

import com.runiversityadmisson.bot.bot.session.BotSession;
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
	public RedisTemplate<String, BotSession> botSessionRedisTemplate(
			RedisConnectionFactory connectionFactory,
			ObjectMapper objectMapper
	) {
		RedisTemplate<String, BotSession> template = new RedisTemplate<>();
		template.setConnectionFactory(connectionFactory);
		template.setKeySerializer(new StringRedisSerializer());
		template.setValueSerializer(new JacksonJsonRedisSerializer<>(objectMapper, BotSession.class));
		template.afterPropertiesSet();
		return template;
	}
}

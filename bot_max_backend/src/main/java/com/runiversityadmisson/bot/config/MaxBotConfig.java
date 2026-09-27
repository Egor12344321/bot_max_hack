package com.runiversityadmisson.bot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.max.botapi.client.MaxBotAPI;

@Configuration
public class MaxBotConfig {

	@Bean(destroyMethod = "close")
	public MaxBotAPI maxBotAPI(@Value("${max.bot.token:}") String token) {
		return MaxBotAPI.create(token);
	}
}

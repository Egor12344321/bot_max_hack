package com.runiversityadmisson.bot.auth;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.runiversityadmisson.bot.web.auth.InvalidInitDataException;
import com.runiversityadmisson.bot.web.auth.MaxInitDataValidator;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MaxInitDataValidatorTest {

	private static final String BOT_TOKEN = "test-bot-token";

	@Test
	void acceptsSignedDataAndReturnsMaxUserId() {
		MaxInitDataValidator validator = validator();

		Long userId = validator.validateAndGetUserId(signedData(Map.of(
				"auth_date", "1771409719",
				"user", "{\"id\":67890,\"first_name\":\"Max\"}"
		)));

		assertThat(userId).isEqualTo(67890L);
	}

	@Test
	void rejectsChangedData() {
		MaxInitDataValidator validator = validator();
		String initData = signedData(Map.of("user", "{\"id\":67890}"));

		assertThatThrownBy(() -> validator.validateAndGetUserId(initData.replace("67890", "67891")))
				.isInstanceOf(InvalidInitDataException.class);
	}

	@Test
	void rejectsDuplicateParameters() {
		MaxInitDataValidator validator = validator();
		String initData = signedData(Map.of("user", "{\"id\":67890}"));

		assertThatThrownBy(() -> validator.validateAndGetUserId(initData + "&user=%7B%22id%22%3A67890%7D"))
				.isInstanceOf(InvalidInitDataException.class);
	}

	private MaxInitDataValidator validator() {
		return new MaxInitDataValidator(BOT_TOKEN, new ObjectMapper());
	}

	private String signedData(Map<String, String> values) {
		try {
			Map<String, String> sortedValues = new TreeMap<>(values);
			String dataCheckString = sortedValues.entrySet().stream()
					.map(entry -> entry.getKey() + "=" + entry.getValue())
					.reduce((left, right) -> left + "\n" + right)
					.orElse("");
			byte[] secretKey = hmac("WebAppData".getBytes(StandardCharsets.UTF_8), BOT_TOKEN.getBytes(StandardCharsets.UTF_8));
			String hash = toHex(hmac(secretKey, dataCheckString.getBytes(StandardCharsets.UTF_8)));
			String parameters = sortedValues.entrySet().stream()
					.map(entry -> entry.getKey() + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
					.reduce((left, right) -> left + "&" + right)
					.orElse("");
			return parameters + "&hash=" + hash;
		} catch (Exception exception) {
			throw new IllegalStateException(exception);
		}
	}

	private byte[] hmac(byte[] key, byte[] message) throws Exception {
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(key, "HmacSHA256"));
		return mac.doFinal(message);
	}

	private String toHex(byte[] bytes) {
		StringBuilder hex = new StringBuilder();
		for (byte value : bytes) {
			hex.append(String.format("%02x", value));
		}
		return hex.toString();
	}
}

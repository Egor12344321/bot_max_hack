package com.runiversityadmisson.bot.web.auth;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class MaxInitDataValidator {

	private static final String HMAC_SHA_256 = "HmacSHA256";
	private final String botToken;
	private final ObjectMapper objectMapper;

	public MaxInitDataValidator(@Value("${max.bot.token}") String botToken, ObjectMapper objectMapper) {
		this.botToken = botToken;
		this.objectMapper = objectMapper;
	}

	public Long validateAndGetUserId(String initData) {
		if (initData == null || initData.isBlank() || botToken.isBlank()) {
			throw new InvalidInitDataException();
		}

		Map<String, String> parameters = parse(initData);
		String actualHash = parameters.remove("hash");
		if (actualHash == null || actualHash.isBlank() || !hasValidSignature(parameters, actualHash)) {
			throw new InvalidInitDataException();
		}

		return userId(parameters.get("user"));
	}

	private Map<String, String> parse(String initData) {
		Map<String, String> parameters = new TreeMap<>();
		for (String pair : initData.split("&", -1)) {
			int separator = pair.indexOf('=');
			if (separator <= 0) {
				throw new InvalidInitDataException();
			}
			String key = pair.substring(0, separator);
			String value = decode(pair.substring(separator + 1));
			if (parameters.putIfAbsent(key, value) != null) {
				throw new InvalidInitDataException();
			}
		}
		return parameters;
	}

	private boolean hasValidSignature(Map<String, String> parameters, String actualHash) {
		try {
			byte[] expectedHash = hexToBytes(actualHash);
			String dataCheckString = parameters.entrySet().stream()
					.map(entry -> entry.getKey() + "=" + entry.getValue())
					.reduce((left, right) -> left + "\n" + right)
					.orElse("");
			byte[] secretKey = hmac("WebAppData".getBytes(StandardCharsets.UTF_8), botToken.getBytes(StandardCharsets.UTF_8));
			byte[] calculatedHash = hmac(secretKey, dataCheckString.getBytes(StandardCharsets.UTF_8));
			return MessageDigest.isEqual(calculatedHash, expectedHash);
		} catch (IllegalArgumentException exception) {
			return false;
		} catch (Exception exception) {
			throw new IllegalStateException("Не удалось проверить данные запуска MAX", exception);
		}
	}

	private Long userId(String user) {
		if (user == null || user.isBlank()) {
			throw new InvalidInitDataException();
		}
		try {
			JsonNode userNode = objectMapper.readTree(user);
			JsonNode id = userNode.get("id");
			if (id == null || !id.canConvertToLong() || id.asLong() <= 0) {
				throw new InvalidInitDataException();
			}
			return id.asLong();
		} catch (InvalidInitDataException exception) {
			throw exception;
		} catch (Exception exception) {
			throw new InvalidInitDataException();
		}
	}

	private String decode(String value) {
		try {
			return URLDecoder.decode(value, StandardCharsets.UTF_8);
		} catch (IllegalArgumentException exception) {
			throw new InvalidInitDataException();
		}
	}

	private byte[] hmac(byte[] key, byte[] message) throws Exception {
		Mac mac = Mac.getInstance(HMAC_SHA_256);
		mac.init(new SecretKeySpec(key, HMAC_SHA_256));
		return mac.doFinal(message);
	}

	private byte[] hexToBytes(String value) {
		if (value.length() != 64) {
			throw new IllegalArgumentException();
		}
		byte[] bytes = new byte[value.length() / 2];
		for (int index = 0; index < value.length(); index += 2) {
			int high = Character.digit(value.charAt(index), 16);
			int low = Character.digit(value.charAt(index + 1), 16);
			if (high < 0 || low < 0) {
				throw new IllegalArgumentException();
			}
			bytes[index / 2] = (byte) ((high << 4) + low);
		}
		return bytes;
	}
}

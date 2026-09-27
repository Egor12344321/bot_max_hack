package com.runiversityadmisson.bot.web.auth;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

	private static final String HMAC_SHA_256 = "HmacSHA256";
	private final byte[] secret;
	private final long ttlSeconds;

	public JwtService(@Value("${jwt.secret}") String secret, @Value("${jwt.ttl}") long ttlSeconds) {
		this.secret = secret.getBytes(StandardCharsets.UTF_8);
		this.ttlSeconds = ttlSeconds;
	}

	public String create(UUID sessionId) {
		long issuedAt = Instant.now().getEpochSecond();
		String header = encode("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
		String payload = encode("{\"sub\":\"" + sessionId + "\",\"iat\":" + issuedAt
				+ ",\"exp\":" + (issuedAt + ttlSeconds) + "}");
		String unsignedToken = header + "." + payload;
		return unsignedToken + "." + encode(sign(unsignedToken));
	}

	public long getTtlSeconds() {
		return ttlSeconds;
	}

	private byte[] sign(String value) {
		try {
			Mac mac = Mac.getInstance(HMAC_SHA_256);
			mac.init(new SecretKeySpec(secret, HMAC_SHA_256));
			return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
		} catch (Exception exception) {
			throw new IllegalStateException("Не удалось подписать JWT", exception);
		}
	}

	private String encode(String value) {
		return encode(value.getBytes(StandardCharsets.UTF_8));
	}

	private String encode(byte[] value) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
	}
}

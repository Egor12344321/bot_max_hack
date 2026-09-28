package com.runiversityadmisson.bot.web.security;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.runiversityadmisson.bot.web.exception.InvalidInitDataException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Валидация initData от MAX WebApp.
 *
 * <p>Алгоритм:
 * <ol>
 *     <li>Разбить initData на пары key=value</li>
 *     <li>Найти и сохранить hash, исключить его из дальнейшей обработки</li>
 *     <li>URL-декодировать значения</li>
 *     <li>Отсортировать по ключам (a → z)</li>
 *     <li>Сформировать строку launch_params (key=value через \n)</li>
 *     <li>secret_key = HMAC-SHA256("WebAppData", BOT_TOKEN)</li>
 *     <li>signature = HMAC-SHA256(secret_key, launch_params)</li>
 *     <li>Сравнить hex(signature) с оригинальным hash</li>
 * </ol>
 *
 * <p>Все ошибки бросаются как {@link InvalidInitDataException} и обрабатываются
 * {@code GlobalExceptionHandler} → 401 Unauthorized.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InitDataValidator {

    private final ObjectMapper objectMapper;

    @Value("${max.bot.token}")
    private String botToken;

    /**
     * Валидирует initData и возвращает user_id.
     *
     * @throws InvalidInitDataException если initData пустой, подпись невалидна
     *                                  или user_id отсутствует
     */
    public Long validateAndExtractUserId(String initData) {
        if (initData == null || initData.isBlank()) {
            throw new InvalidInitDataException("initData is empty");
        }

        List<String[]> params = parse(initData);

        if (!isValid(params)) {
            throw new InvalidInitDataException("initData signature is invalid");
        }

        return extractUserId(params);
    }

    /**
     * Проверяет подпись initData.
     *
     * @param params распарсенные параметры initData
     * @return true, если подпись совпадает с оригинальным hash
     */
    private boolean isValid(List<String[]> params) {
        String originalHash = params.stream()
                .filter(p -> "hash".equals(p[0]))
                .map(p -> p[1])
                .findFirst()
                .orElseThrow(() -> new InvalidInitDataException("hash not found"));

        String launchParams = params.stream()
                .filter(p -> !"hash".equals(p[0]))
                .sorted(Comparator.comparing(p -> p[0]))
                .map(p -> p[0] + "=" + p[1])
                .collect(Collectors.joining("\n"));

        byte[] secretKey = hmacSha256(
                "WebAppData".getBytes(StandardCharsets.UTF_8),
                botToken.getBytes(StandardCharsets.UTF_8)
        );

        byte[] signature = hmacSha256(
                secretKey,
                launchParams.getBytes(StandardCharsets.UTF_8)
        );

        String computedHash = bytesToHex(signature);

        if (!computedHash.equalsIgnoreCase(originalHash)) {
            log.warn("initData: signature mismatch");
            return false;
        }
        return true;
    }

    /**
     * Извлекает user_id из поля user.
     *
     * @throws InvalidInitDataException если поле user или user.id отсутствует
     */
    private Long extractUserId(List<String[]> params) {
        String userJson = params.stream()
                .filter(p -> "user".equals(p[0]))
                .map(p -> p[1])
                .findFirst()
                .orElseThrow(() -> new InvalidInitDataException("user field not found"));

        JsonNode userNode = readTree(userJson);
        JsonNode idNode = userNode.get("id");

        if (idNode == null) {
            throw new InvalidInitDataException("user.id not found");
        }
        return idNode.asLong();
    }

    /**
     * Разбивает initData на пары key/value с URL-декодированием.
     *
     * @throws InvalidInitDataException если есть дубликаты параметров
     */
    private List<String[]> parse(String initData) {
        List<String[]> params = new ArrayList<>();
        Map<String, Boolean> seen = new LinkedHashMap<>();

        for (String pair : initData.split("&")) {
            int idx = pair.indexOf("=");
            if (idx <= 0) {
                continue;
            }
            String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
            String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);

            if (seen.putIfAbsent(key, true) != null) {
                throw new InvalidInitDataException("Duplicate parameter: " + key);
            }
            params.add(new String[]{key, value});
        }
        return params;
    }

    /** Парсит JSON, оборачивая IOException в доменное исключение. */
    private JsonNode readTree(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception exception) {
            throw new InvalidInitDataException("Failed to parse user JSON", exception);
        }
    }

    /** HMAC-SHA256, оборачивая checked-исключения. */
    private byte[] hmacSha256(byte[] key, byte[] message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(message);
        } catch (NoSuchAlgorithmException | InvalidKeyException exception) {
            throw new IllegalStateException("HMAC-SHA256 not available", exception);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}

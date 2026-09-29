package com.runiversityadmisson.bot.application.benefit;

import com.runiversityadmisson.bot.domain.applicant.model.benefit.PrivilegeCategory;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

/**
 * Справочник льгот, которого пока нет в БД.
 * Тестовые данные читаются один раз при старте из classpath:catalog/privilege-categories.json.
 * Когда появится таблица — достаточно заменить загрузку, контракт сервиса не изменится.
 */
@Service
public class PrivilegeCatalogService {

	static final String PRIVILEGE_CATEGORIES_PATH = "catalog/privilege-categories.json";

	private final Map<String, PrivilegeCategory> privilegeCategories;

	public PrivilegeCatalogService(ObjectMapper objectMapper) {
		this.privilegeCategories = indexById(
				read(objectMapper, PRIVILEGE_CATEGORIES_PATH, PrivilegeCategory[].class), PrivilegeCategory::id);
	}

	public List<PrivilegeCategory> getPrivilegeCategories() {
		return List.copyOf(privilegeCategories.values());
	}

	public Optional<PrivilegeCategory> findPrivilegeCategory(String id) {
		return Optional.ofNullable(privilegeCategories.get(id));
	}

	private static <T> T[] read(ObjectMapper objectMapper, String path, Class<T[]> type) {
		try (InputStream input = new ClassPathResource(path).getInputStream()) {
			return objectMapper.readValue(input, type);
		} catch (IOException exception) {
			throw new IllegalStateException("Не удалось прочитать справочник " + path, exception);
		}
	}

	private static <T> Map<String, T> indexById(T[] items, Function<T, String> idGetter) {
		return Arrays.stream(items).collect(Collectors.toMap(idGetter, Function.identity(),
				(first, second) -> {
					throw new IllegalStateException("Дублирующийся id в справочнике: " + idGetter.apply(first));
				},
				LinkedHashMap::new));
	}
}

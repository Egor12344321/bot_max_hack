package com.runiversityadmisson.bot.application.direction;

import com.runiversityadmisson.bot.domain.applicant.model.direction.Direction;
import com.runiversityadmisson.bot.domain.applicant.model.profile.User;
import com.runiversityadmisson.bot.domain.applicant.ports.direction.DirectionRepository;
import com.runiversityadmisson.bot.domain.applicant.ports.direction.InterestCategoryRepository;
import com.runiversityadmisson.bot.domain.applicant.ports.profile.UserRepository;
import com.runiversityadmisson.bot.presentation.exception.BadRequestException;
import com.runiversityadmisson.bot.presentation.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DirectionServiceTest {
	private final DirectionRepository directions = mock(DirectionRepository.class);
	private final InterestCategoryRepository categories = mock(InterestCategoryRepository.class);
	private final UserRepository users = mock(UserRepository.class);
	private final DirectionService service = new DirectionService(directions, categories, users);
	private final UUID id = UUID.randomUUID();

	@Test
	void categoryAndQueryAreCombinedAndTotalIsBeforePagination() {
		when(categories.existsById("it")).thenReturn(true);
		when(directions.findAll()).thenReturn(List.of(
				direction("09.03.04", "Программная инженерия", "it"),
				direction("09.03.01", "Информатика и вычислительная техника", "it"),
				direction("03.03.02", "Физика", "physics")));
		var page = service.search("it", "  ИН  ", 1, 1);
		assertEquals(2, page.total());
		assertEquals("09.03.04", page.items().getFirst().id());
		assertTrue(service.search("it", "нет совпадений", 0, 20).items().isEmpty());
		assertTrue(service.search("it", null, Integer.MAX_VALUE, 20).items().isEmpty());
		assertEquals(1, service.search(null, "03.03", 0, 20).total());
	}

	@Test
	void rejectsUnknownCategoryAndInvalidPagination() {
		assertThrows(BadRequestException.class, () -> service.search("unknown", null, 0, 20));
		assertThrows(BadRequestException.class, () -> service.search(null, null, -1, 20));
		assertThrows(BadRequestException.class, () -> service.search(null, null, 0, 0));
		assertThrows(BadRequestException.class, () -> service.search(null, null, 0, 101));
		verifyNoInteractions(directions);
	}

	@Test
	void replacesRestoresOrderAndClearsSelection() {
		User user = new User();
		when(users.findById(id)).thenReturn(Optional.of(user));
		var ids = List.of("09.03.04", "09.03.01");
		when(directions.findAllById(ids)).thenReturn(List.of(
				direction("09.03.01", "ИВТ", "it"), direction("09.03.04", "ПИ", "it")));
		assertEquals(List.of(), service.getSelection(id).directionIds());
		assertEquals(ids, service.replaceSelection(id, ids).directionIds());
		assertEquals(ids, service.getSelection(id).directionIds());
		assertEquals(List.of(), service.replaceSelection(id, List.of()).directionIds());
		assertTrue(user.getDirectionIds().isEmpty());
	}

	@Test
	void invalidReplacementDoesNotClearPreviousSelection() {
		User user = new User();
		user.getDirectionIds().add("09.03.04");
		when(users.findById(id)).thenReturn(Optional.of(user));
		assertThrows(BadRequestException.class,
				() -> service.replaceSelection(id, List.of("unknown")));
		assertThrows(BadRequestException.class,
				() -> service.replaceSelection(id, List.of("09.03.04", "09.03.04")));
		assertEquals(List.of("09.03.04"), user.getDirectionIds());
	}

	@Test
	void rejectsMissingSession() {
		when(users.findById(id)).thenReturn(Optional.empty());
		assertThrows(ResourceNotFoundException.class, () -> service.getSelection(id));
		assertThrows(ResourceNotFoundException.class, () -> service.replaceSelection(id, List.of()));
	}

	private Direction direction(String code, String name, String category) {
		Direction direction = new Direction();
		direction.setId(code);
		direction.setCode(code);
		direction.setName(name);
		direction.setInterestCategoryIds(Set.of(category));
		return direction;
	}
}

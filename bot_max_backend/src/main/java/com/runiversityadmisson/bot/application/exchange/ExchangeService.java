package com.runiversityadmisson.bot.application.exchange;

import com.runiversityadmisson.bot.application.dto.response.ExchangeResponse;
import com.runiversityadmisson.bot.domain.applicant.model.User;
import com.runiversityadmisson.bot.domain.applicant.ports.UserRepository;
import com.runiversityadmisson.bot.presentation.exception.ResourceNotFoundException;
import com.runiversityadmisson.bot.presentation.security.InitDataValidator;
import com.runiversityadmisson.bot.presentation.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeService {

    private final InitDataValidator initDataValidator;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Transactional
    public ExchangeResponse exchange(String launchParams) {
        Long maxUserId = initDataValidator.validateAndExtractUserId(launchParams);
		log.info("Exchange: подпись MAX launchParams подтверждена");

        User user = userRepository.findByMaxUserId(maxUserId)
                .orElseThrow(() -> {
					log.warn("Exchange: для пользователя MAX нет завершённой заявки");
					return new ResourceNotFoundException("Завершённая заявка не найдена");
				});

        String accessToken = jwtService.generate(user.getId());
		log.info("Exchange: JWT сформирован для завершённой заявки");

        return new ExchangeResponse(
                accessToken,
                Math.toIntExact(jwtService.getTtlSeconds()),
                new ExchangeResponse.SessionResponse(
                        user.getId(),
                        "max",
                        user.getLanguage(),
                        user.getCitizenship(),
                        user.getCreatedAt()
                )
        );
    }
}

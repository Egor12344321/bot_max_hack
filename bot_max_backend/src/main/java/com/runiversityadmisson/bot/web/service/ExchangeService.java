package com.runiversityadmisson.bot.web.service;

import com.runiversityadmisson.bot.web.auth.JwtService;
import com.runiversityadmisson.bot.web.dto.ExchangeResponse;
import com.runiversityadmisson.bot.web.exception.ResourceNotFoundException;
import com.runiversityadmisson.bot.web.model.User;
import com.runiversityadmisson.bot.web.repository.UserRepository;
import com.runiversityadmisson.bot.web.security.InitDataValidator;
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

        User user = userRepository.findByMaxUserId(maxUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Завершённая заявка не найдена"));

        String accessToken = jwtService.generate(user.getId());

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

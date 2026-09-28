package com.runiversityadmisson.bot.domain.applicant.service;

import com.runiversityadmisson.bot.domain.applicant.model.EgeScore;
import com.runiversityadmisson.bot.domain.applicant.model.User;
import com.runiversityadmisson.bot.domain.applicant.ports.EgeScoreRepository;
import com.runiversityadmisson.bot.domain.applicant.ports.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final EgeScoreRepository egeScoreRepository;

    @Transactional
    public void syncFromBot(Long maxUserId, String language, String citizenship,
                            String track, Map<String, Integer> egeScores) {
        User user = userRepository.findByMaxUserId(maxUserId)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setMaxUserId(maxUserId);
                    return newUser;
                });

        user.setLanguage(language);
        user.setCitizenship(citizenship);
        user.setTrack(track);
        userRepository.save(user);

        egeScoreRepository.deleteByUserId(user.getId());
        for (Map.Entry<String, Integer> entry : egeScores.entrySet()) {
            EgeScore score = new EgeScore();
            score.setUser(user);
            score.setSubjectId(entry.getKey());
            score.setScore(entry.getValue());
            egeScoreRepository.save(score);
        }
    }

    public User getOrCreate(Long maxUserId) {
        return userRepository.findByMaxUserId(maxUserId)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setMaxUserId(maxUserId);
                    return userRepository.save(newUser);
                });
    }

    @Transactional
    public void deleteByMaxUserId(Long maxUserId) {
        userRepository.deleteByMaxUserId(maxUserId);
    }
}

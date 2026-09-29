package com.runiversityadmisson.bot.application.max.dialog;


import java.util.HashMap;
import java.util.Map;
import lombok.Data;

@Data
public class BotQuestionnaire {

    private Long userId;
    private String language;
    private String citizenship;
    private String track;
    private BotQuestionnaireStep state = BotQuestionnaireStep.NEW;
    private Map<String, Integer> egeScores = new HashMap<>();
    private String currentSubject;
}

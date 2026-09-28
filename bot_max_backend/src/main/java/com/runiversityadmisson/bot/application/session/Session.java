package com.runiversityadmisson.bot.application.session;


import java.util.HashMap;
import java.util.Map;
import lombok.Data;

@Data
public class Session {

    private Long userId;
    private String language;
    private String citizenship;
    private String track;
    private SessionState state = SessionState.NEW;
    private Map<String, Integer> egeScores = new HashMap<>();
    private String currentSubject;
}

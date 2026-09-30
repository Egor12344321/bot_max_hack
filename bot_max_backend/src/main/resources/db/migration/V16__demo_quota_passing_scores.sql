-- Демонстрационные проходные по квотам: реальных данных вузов по квотам в базе нет.
-- Считаются от проходного общего конкурса того же года (целевая 78%, особая 82%, отдельная 88%)
-- и не ниже суммы минимальных порогов испытаний программы. Помечаются data_source = 'demo'.
-- Если проходной общего конкурса неизвестен (все места заняли БВИ), проходной квоты остаётся пустым.
WITH minimum AS (
    SELECT program_id, SUM(group_minimum) AS total
    FROM (
        SELECT ps.program_id, MIN(s.min_threshold) AS group_minimum
        FROM program_subjects ps
        JOIN subjects s ON s.id = ps.subject_id
        GROUP BY ps.program_id, COALESCE('group:' || ps.choice_group, 'subject:' || ps.subject_id)
    ) groups
    GROUP BY program_id
)
UPDATE program_competitions c
SET passing_score = GREATEST(
        ROUND(p.passing_score_previous_year * CASE c.competition_type
            WHEN 'target_quota' THEN 0.78
            WHEN 'special_quota' THEN 0.82
            ELSE 0.88
        END)::INTEGER,
        COALESCE(m.total, 0)),
    previous_year = p.passing_score_year,
    data_source = 'demo'
FROM programs p
LEFT JOIN minimum m ON m.program_id = p.id
WHERE c.program_id = p.id
  AND c.competition_type <> 'general'
  AND c.passing_score IS NULL
  AND p.passing_score_previous_year IS NOT NULL;

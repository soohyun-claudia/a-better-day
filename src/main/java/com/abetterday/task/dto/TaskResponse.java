package com.abetterday.task.dto;

import com.abetterday.task.domain.TaskPriority;
import com.abetterday.task.domain.TaskStatus;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

/** task 응답. dueLabel과 urgent는 서버(DueLabeler)가 계산한 값이다. */
public record TaskResponse(
        Long id,
        String title,
        TaskStatus status,
        TaskPriority priority,
        String category,
        LocalDate dueDate,
        LocalTime dueTime,
        String dueLabel,
        boolean urgent,
        OffsetDateTime doneAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}

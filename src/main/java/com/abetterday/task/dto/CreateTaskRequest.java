package com.abetterday.task.dto;

import com.abetterday.task.domain.TaskPriority;
import com.abetterday.task.domain.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

/** task 생성 요청. 생성 시 status는 WISH, TODO만 허용한다(서비스에서 검사). */
public record CreateTaskRequest(
        @NotBlank(message = "제목을 입력해 주세요.")
        @Size(max = 200, message = "제목은 200자 이내로 입력해 주세요.")
        String title,
        @NotNull(message = "상태를 입력해 주세요.")
        TaskStatus status,
        TaskPriority priority,
        @Size(max = 50, message = "카테고리는 50자 이내로 입력해 주세요.")
        String category,
        LocalDate dueDate,
        LocalTime dueTime) {

    /** 제목과 카테고리의 앞뒤 공백을 제거한다. */
    public CreateTaskRequest {
        title = title == null ? null : title.strip();
        category = category == null ? null : category.strip();
    }
}

package com.abetterday.task.dto;

import com.abetterday.task.domain.TaskStatus;
import jakarta.validation.constraints.NotNull;

/** task 상태 변경 요청. */
public record ChangeStatusRequest(@NotNull(message = "상태를 입력해 주세요.") TaskStatus status) {
}

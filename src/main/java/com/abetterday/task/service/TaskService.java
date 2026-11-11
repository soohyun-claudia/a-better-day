package com.abetterday.task.service;

import com.abetterday.common.exception.BusinessException;
import com.abetterday.task.domain.Task;
import com.abetterday.task.domain.TaskPriority;
import com.abetterday.task.domain.TaskStatus;
import com.abetterday.task.dto.CreateTaskRequest;
import com.abetterday.task.dto.TaskResponse;
import com.abetterday.task.repository.TaskRepository;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** task 생성·조회·상태 전환을 담당하는 서비스. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

    private final TaskRepository taskRepository;
    private final DueLabeler dueLabeler;
    private final Clock clock;

    /** task를 생성한다. 생성 시 상태는 WISH, TODO만 허용한다. */
    @Transactional
    public TaskResponse createTask(CreateTaskRequest request) {
        if (request.status() != TaskStatus.WISH && request.status() != TaskStatus.TODO) {
            throw new BusinessException("INVALID_CREATE_STATUS",
                    "할 일은 하고 싶은 일 또는 할 일 상태로만 만들 수 있어요.", HttpStatus.BAD_REQUEST);
        }
        // DB check 제약에 걸리기 전에 사용자에게 읽히는 메시지로 막는다.
        if (request.dueTime() != null && request.dueDate() == null) {
            throw new BusinessException("DUE_TIME_WITHOUT_DATE",
                    "마감 시각은 마감 날짜와 함께 입력해 주세요.", HttpStatus.BAD_REQUEST);
        }
        TaskPriority priority = request.priority() == null ? TaskPriority.MEDIUM : request.priority();
        String category = request.category() == null || request.category().isEmpty() ? null : request.category();

        Task task = Task.create(request.title(), request.status(), priority, category,
                request.dueDate(), request.dueTime());
        Task saved = taskRepository.saveAndFlush(task);
        log.info("task 생성: id={}, status={}", saved.getId(), saved.getStatus());
        return toResponse(saved);
    }

    /** 상태 목록으로 task를 조회한다. 목록이 비어 있으면 전체를 반환한다. */
    public List<TaskResponse> getTasks(List<TaskStatus> statuses) {
        List<Task> tasks = statuses == null || statuses.isEmpty()
                ? taskRepository.findAll()
                : taskRepository.findByStatusIn(statuses);
        return tasks.stream().map(this::toResponse).toList();
    }

    /** 서울 기준 오늘 완료한 task를 완료 시각 내림차순으로 조회한다. */
    public List<TaskResponse> getDoneToday() {
        OffsetDateTime start = LocalDate.now(clock).atStartOfDay(clock.getZone()).toOffsetDateTime();
        OffsetDateTime end = start.plusDays(1);
        return taskRepository.findDoneToday(start, end).stream().map(this::toResponse).toList();
    }

    /**
     * task의 상태를 바꾼다. 허용되지 않는 전환은 409다.
     * DOING 전환은 기존 DOING을 먼저 TODO로 저장·flush한 뒤 대상을 바꾼다.
     */
    @Transactional
    public TaskResponse changeStatus(Long id, TaskStatus newStatus) {
        Task task = taskRepository.findById(id).orElseThrow(() ->
                new BusinessException("TASK_NOT_FOUND", "해당 할 일을 찾을 수 없어요.", HttpStatus.NOT_FOUND));
        checkTransition(task.getStatus(), newStatus);

        switch (newStatus) {
            case DOING -> {
                // 유니크 인덱스는 문장 단위로 검사되므로 기존 DOING을 먼저 flush해야 한다.
                taskRepository.findFirstByStatus(TaskStatus.DOING).ifPresent(current -> {
                    current.markTodo();
                    taskRepository.saveAndFlush(current);
                });
                task.markDoing();
            }
            case DONE -> task.markDone(OffsetDateTime.now(clock));
            case TODO -> task.promote(LocalDate.now(clock).with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)));
            default -> throw transitionNotAllowed();
        }
        Task saved = taskRepository.saveAndFlush(task);
        log.info("task 상태 변경: id={}, status={}", saved.getId(), saved.getStatus());
        return toResponse(saved);
    }

    /** 현재 상태에서 새 상태로 바꿀 수 있는지 검사한다. 불가능하면 409를 던진다. */
    private void checkTransition(TaskStatus current, TaskStatus next) {
        boolean allowed = switch (next) {
            case DONE -> current == TaskStatus.TODO || current == TaskStatus.DOING;
            case DOING -> current == TaskStatus.TODO;
            case TODO -> current == TaskStatus.WISH;
            case WISH -> false;
        };
        if (!allowed) {
            log.warn("허용되지 않는 상태 전환: {} -> {}", current, next);
            throw transitionNotAllowed();
        }
    }

    /** 상태 전환 불가 예외(409)를 만든다. */
    private BusinessException transitionNotAllowed() {
        return new BusinessException("INVALID_STATUS_TRANSITION",
                "지금 상태에서는 바꿀 수 없어요.", HttpStatus.CONFLICT);
    }

    /** task를 응답 DTO로 바꾸면서 dueLabel과 urgent를 계산해 담는다. */
    private TaskResponse toResponse(Task task) {
        DueLabeler.Due due = dueLabeler.calculate(task);
        return new TaskResponse(task.getId(), task.getTitle(), task.getStatus(), task.getPriority(),
                task.getCategory(), task.getDueDate(), task.getDueTime(), due.label(), due.urgent(),
                task.getDoneAt(), task.getCreatedAt(), task.getUpdatedAt());
    }
}

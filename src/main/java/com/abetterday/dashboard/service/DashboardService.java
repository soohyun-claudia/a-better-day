package com.abetterday.dashboard.service;

import com.abetterday.dashboard.dto.DashboardResponse;
import com.abetterday.dashboard.dto.DashboardResponse.DonePanel;
import com.abetterday.dashboard.dto.DashboardResponse.ProgressData;
import com.abetterday.dashboard.dto.DashboardResponse.TodayTodoPanel;
import com.abetterday.dashboard.dto.DashboardResponse.TopBar;
import com.abetterday.dashboard.dto.DashboardResponse.WishPanel;
import com.abetterday.task.domain.TaskStatus;
import com.abetterday.task.dto.TaskResponse;
import com.abetterday.task.service.TaskService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 화면 전체 조회 데이터를 조립하는 서비스. task 서비스의 결과를 조합만 한다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private static final int MAX_TODO_ITEMS = 7;
    private static final int MAX_DONE_ITEMS = 4;
    private static final int MAX_WISH_ITEMS = 4;
    private static final DateTimeFormatter DATE_LABEL_FORMAT =
            DateTimeFormatter.ofPattern("yyyy년 M월 d일 (E)", Locale.KOREAN);

    /** 오늘 할 일 정렬: DOING 먼저 → 우선순위 → 마감 빠른 순(없음은 뒤) → 먼저 만든 순. */
    private static final Comparator<TaskResponse> TODO_ORDER = Comparator
            .comparing((TaskResponse t) -> t.status() == TaskStatus.DOING ? 0 : 1)
            .thenComparing(TaskResponse::priority)
            .thenComparing(TaskResponse::dueDate, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(TaskResponse::dueTime, Comparator.nullsLast(Comparator.<LocalTime>naturalOrder()))
            .thenComparing(TaskResponse::createdAt);

    private final TaskService taskService;
    private final Clock clock;

    /** 화면 전체 데이터를 한 번에 조립한다. */
    public DashboardResponse getDashboard() {
        List<TaskResponse> todos = taskService.getTasks(List.of(TaskStatus.TODO, TaskStatus.DOING)).stream()
                .sorted(TODO_ORDER).toList();
        List<TaskResponse> dones = taskService.getDoneToday();
        List<TaskResponse> wishes = taskService.getTasks(List.of(TaskStatus.WISH)).stream()
                .sorted(Comparator.comparing(TaskResponse::createdAt)).toList();

        return new DashboardResponse(
                buildTopBar(todos, dones.size()),
                new TodayTodoPanel(limit(todos, MAX_TODO_ITEMS), todos.size(), hidden(todos, MAX_TODO_ITEMS)),
                new DonePanel(limit(dones, MAX_DONE_ITEMS), dones.size()),
                new WishPanel(limit(wishes, MAX_WISH_ITEMS), wishes.size(), hidden(wishes, MAX_WISH_ITEMS)));
    }

    /** 상단 바(오늘 날짜, 집중할 일, 진행률)를 만든다. */
    private TopBar buildTopBar(List<TaskResponse> todos, int doneCount) {
        LocalDate today = LocalDate.now(clock);
        TaskResponse doing = todos.stream()
                .filter(t -> t.status() == TaskStatus.DOING).findFirst().orElse(null);
        return new TopBar(today.toString(), today.format(DATE_LABEL_FORMAT), doing,
                buildProgress(doneCount, todos.size()));
    }

    /** 진행률을 계산한다. 분모는 남은 TODO·DOING 수와 오늘 완료 수의 합이고, 0이면 0%다. */
    private ProgressData buildProgress(int doneCount, int remainingCount) {
        int total = doneCount + remainingCount;
        int percent = total == 0 ? 0 : Math.round(doneCount * 100f / total);
        return new ProgressData(doneCount, total, percent);
    }

    /** 목록의 앞쪽 max개만 돌려준다. */
    private List<TaskResponse> limit(List<TaskResponse> items, int max) {
        return items.stream().limit(max).toList();
    }

    /** 표시 개수를 넘어 숨겨지는 건수를 계산한다. */
    private int hidden(List<TaskResponse> items, int max) {
        return Math.max(0, items.size() - max);
    }
}

package com.abetterday.dashboard.dto;

import com.abetterday.task.dto.TaskResponse;
import java.util.List;

/** 화면 전체 데이터. 정렬·개수 제한·집계는 모두 서버가 계산해 담는다. */
public record DashboardResponse(TopBar topBar, TodayTodoPanel todayTodo, DonePanel done, WishPanel wish) {

    /** 상단 바. today는 서울 기준 ISO 날짜이고, doingTask는 없으면 null이다. */
    public record TopBar(String today, String dateLabel, TaskResponse doingTask, ProgressData progress) {
    }

    /** 오늘 진행률. total이 0이면 percent도 0이다. */
    public record ProgressData(int done, int total, int percent) {
    }

    /** 오늘 할 일 패널. totalCount는 남은 TODO·DOING 전체 수다. */
    public record TodayTodoPanel(List<TaskResponse> items, int totalCount, int hiddenCount) {
    }

    /** 한 일 패널. doneCount는 오늘 완료한 전체 수다. */
    public record DonePanel(List<TaskResponse> items, int doneCount) {
    }

    /** 하고 싶은 일 패널. */
    public record WishPanel(List<TaskResponse> items, int totalCount, int hiddenCount) {
    }
}

package com.abetterday.task.service;

import com.abetterday.task.domain.Task;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** task의 마감 표기 문구(dueLabel)와 긴급 여부(urgent)를 계산하는 유일한 곳. */
@Component
@RequiredArgsConstructor
public class DueLabeler {

    private static final long URGENT_THRESHOLD_MINUTES = 120;
    private static final long MINUTES_PER_HOUR = 60;
    private static final long MINUTES_PER_DAY = 1440;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final Clock clock;

    /** 계산된 마감 표기 문구와 긴급 여부. */
    public record Due(String label, boolean urgent) {
    }

    /** task의 마감 표기 문구와 긴급 여부를 계산한다. */
    public Due calculate(Task task) {
        LocalDate dueDate = task.getDueDate();
        if (dueDate == null) {
            return new Due("마감 없음", false);
        }
        if (task.getDueTime() == null) {
            return new Due(dateOnlyLabel(dueDate), false);
        }
        return timedDue(dueDate, task);
    }

    /**
     * 시각이 없는 마감의 문구를 만든다.
     * 우선순위: 지남 → 오늘 중 → 내일 → 이번 주 일요일 → M/D 마감
     */
    private String dateOnlyLabel(LocalDate dueDate) {
        LocalDate today = LocalDate.now(clock);
        LocalDate thisSunday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        if (dueDate.isBefore(today)) {
            return monthDay(dueDate) + " 마감";
        }
        if (dueDate.equals(today)) {
            return "오늘 중";
        }
        if (dueDate.equals(today.plusDays(1))) {
            return "내일 마감";
        }
        if (dueDate.equals(thisSunday)) {
            return "이번 주 마감";
        }
        return monthDay(dueDate) + " 마감";
    }

    /**
     * 시각이 있는 마감의 문구와 긴급 여부를 만든다.
     * 절삭하지 않은 두 시각의 차이를 먼저 구하고, 그 차이의 초를 0 방향으로 버린 분 값을 문구와 긴급 판정에 함께 쓴다.
     */
    private Due timedDue(LocalDate dueDate, Task task) {
        LocalDateTime target = dueDate.atTime(task.getDueTime());
        LocalDateTime now = LocalDateTime.now(clock);
        long diff = Duration.between(now, target).toMinutes();

        String label = datePrefix(dueDate) + target.format(TIME_FORMAT) + " 마감" + remainSuffix(diff);
        return new Due(label, diff < URGENT_THRESHOLD_MINUTES);
    }

    /** 마감 날짜 접두어를 만든다. 오늘은 생략, 내일은 "내일 ", 그 외는 "M/D "다. */
    private String datePrefix(LocalDate dueDate) {
        LocalDate today = LocalDate.now(clock);
        if (dueDate.equals(today)) {
            return "";
        }
        if (dueDate.equals(today.plusDays(1))) {
            return "내일 ";
        }
        return monthDay(dueDate) + " ";
    }

    /**
     * 남은 분(음수면 지난 분)을 " · N분 남음" 같은 접미 문구로 바꾼다.
     * 2시간 이상 남았으면 접미 문구를 붙이지 않는다.
     */
    private String remainSuffix(long diff) {
        if (diff < 0) {
            return " · " + overdueText(-diff) + " 지남";
        }
        if (diff == 0) {
            return " · 지금 마감";
        }
        if (diff < MINUTES_PER_HOUR) {
            return " · " + diff + "분 남음";
        }
        if (diff < URGENT_THRESHOLD_MINUTES) {
            long minutes = diff - MINUTES_PER_HOUR;
            return minutes == 0 ? " · 1시간 남음" : " · 1시간 " + minutes + "분 남음";
        }
        return "";
    }

    /** 지난 시간을 "N분", "N시간 M분", "N일"로 표기한다. */
    private String overdueText(long minutes) {
        if (minutes < MINUTES_PER_HOUR) {
            return minutes + "분";
        }
        if (minutes < MINUTES_PER_DAY) {
            long hours = minutes / MINUTES_PER_HOUR;
            long rest = minutes % MINUTES_PER_HOUR;
            return rest == 0 ? hours + "시간" : hours + "시간 " + rest + "분";
        }
        return (minutes / MINUTES_PER_DAY) + "일";
    }

    /** 날짜를 "M/D" 형식으로 바꾼다. */
    private String monthDay(LocalDate date) {
        return date.getMonthValue() + "/" + date.getDayOfMonth();
    }
}

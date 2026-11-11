package com.abetterday.task.service;

import com.abetterday.task.domain.Task;
import com.abetterday.task.domain.TaskPriority;
import com.abetterday.task.domain.TaskStatus;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/** 고정된 서울 시각으로 T-19의 확정된 마감 문구와 긴급 경계를 검증한다. */
class DueLabelerTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    /** 남은 시간과 지난 시간의 분·시간·일 경계에서 전체 문구와 긴급 여부를 검증한다. */
    @DisplayName("시각 마감의 120·119·60·0·-1분 및 지난 시간 단위 경계")
    @ParameterizedTest(name = "{0}분 차이: {1}, 긴급={2}")
    @CsvSource({
        "121, 14:01 마감, false",
        "120, 14:00 마감, false",
        "119, 13:59 마감 · 1시간 59분 남음, true",
        "61, 13:01 마감 · 1시간 1분 남음, true",
        "60, 13:00 마감 · 1시간 남음, true",
        "59, 12:59 마감 · 59분 남음, true",
        "1, 12:01 마감 · 1분 남음, true",
        "0, 12:00 마감 · 지금 마감, true",
        "-1, 11:59 마감 · 1분 지남, true",
        "-59, 11:01 마감 · 59분 지남, true",
        "-60, 11:00 마감 · 1시간 지남, true",
        "-61, 10:59 마감 · 1시간 1분 지남, true",
        "-120, 10:00 마감 · 2시간 지남, true",
        "-1439, 11/10 12:01 마감 · 23시간 59분 지남, true",
        "-1440, 11/10 12:00 마감 · 1일 지남, true",
        "-1501, 11/10 10:59 마감 · 1일 지남, true",
        "-2880, 11/9 12:00 마감 · 2일 지남, true"
    })
    void calculatesMinuteBoundaries(long minutes, String expectedLabel, boolean expectedUrgent) {
        // given: 초가 없는 기존 분 경계 사례도 회귀 검증으로 유지한다.
        LocalDateTime now = LocalDateTime.parse("2026-11-11T12:00:00");
        LocalDateTime target = now.plusMinutes(minutes);
        DueLabeler labeler = labelerAt(now);
        Task task = taskDue(target.toLocalDate(), target.toLocalTime());

        // when
        DueLabeler.Due actual = labeler.calculate(task);

        // then
        assertThat(actual).isEqualTo(new DueLabeler.Due(expectedLabel, expectedUrgent));
    }

    /** 시각 마감의 날짜 접두어와 서울 자정 전후의 남음·지남을 검증한다. */
    @DisplayName("오늘·내일·그 외 날짜 접두어와 서울 자정 경계")
    @ParameterizedTest(name = "현재 {0}, 마감 {1}: {2}")
    @CsvSource({
        "2026-11-11T12:00, 2026-11-12T15:00, 내일 15:00 마감, false",
        "2026-11-11T12:00, 2026-11-13T15:00, 11/13 15:00 마감, false",
        "2026-11-11T23:59, 2026-11-12T00:01, 내일 00:01 마감 · 2분 남음, true",
        "2026-11-12T00:00, 2026-11-12T00:01, 00:01 마감 · 1분 남음, true",
        "2026-11-12T00:01, 2026-11-11T23:59, 11/11 23:59 마감 · 2분 지남, true",
        "2026-11-15T23:59, 2026-11-16T00:01, 내일 00:01 마감 · 2분 남음, true",
        "2026-12-31T23:59, 2027-01-01T00:01, 내일 00:01 마감 · 2분 남음, true"
    })
    void calculatesTimedDateBoundaries(String now, String target, String expectedLabel, boolean expectedUrgent) {
        // given
        LocalDateTime due = LocalDateTime.parse(target);
        DueLabeler labeler = labelerAt(LocalDateTime.parse(now));
        Task task = taskDue(due.toLocalDate(), due.toLocalTime());

        // when
        DueLabeler.Due actual = labeler.calculate(task);

        // then
        assertThat(actual).isEqualTo(new DueLabeler.Due(expectedLabel, expectedUrgent));
    }

    /** 날짜 전용 마감은 과거 여부와 관계없이 긴급하지 않으며 확정된 날짜 문구를 반환하는지 검증한다. */
    @DisplayName("날짜만 있는 마감의 날짜·주·자정 경계 및 긴급 아님")
    @ParameterizedTest(name = "현재 {0}, 마감일 {1}: {2}")
    @CsvSource({
        "2026-11-11T12:00, 2026-11-10, 11/10 마감",
        "2026-11-11T12:00, 2026-11-11, 오늘 중",
        "2026-11-11T12:00, 2026-11-12, 내일 마감",
        "2026-11-11T12:00, 2026-11-15, 이번 주 마감",
        "2026-11-11T12:00, 2026-11-13, 11/13 마감",
        "2026-11-11T12:00, 2026-11-22, 11/22 마감",
        "2026-11-14T12:00, 2026-11-15, 내일 마감",
        "2026-11-15T12:00, 2026-11-16, 내일 마감",
        "2026-11-16T00:01, 2026-11-15, 11/15 마감",
        "2026-11-16T00:01, 2026-11-22, 이번 주 마감",
        "2026-11-11T23:59, 2026-11-12, 내일 마감",
        "2026-11-12T00:00, 2026-11-12, 오늘 중",
        "2026-11-12T00:01, 2026-11-11, 11/11 마감"
    })
    void calculatesDateOnlyBoundaries(String now, String dueDate, String expectedLabel) {
        // given
        DueLabeler labeler = labelerAt(LocalDateTime.parse(now));
        Task task = taskDue(LocalDate.parse(dueDate), null);

        // when
        DueLabeler.Due actual = labeler.calculate(task);

        // then
        assertThat(actual).isEqualTo(new DueLabeler.Due(expectedLabel, false));
    }

    /** 기존 회귀 사례로 일요일 당일 날짜 전용 마감의 긴급 여부를 검증한다. */
    @Test
    @DisplayName("일요일 당일 날짜 전용 마감도 긴급하지 않음")
    void sundayDateOnlyIsNeverUrgent() {
        // given
        DueLabeler labeler = labelerAt(LocalDateTime.parse("2026-11-15T12:00"));
        Task task = taskDue(LocalDate.parse("2026-11-15"), null);

        // when
        DueLabeler.Due actual = labeler.calculate(task);

        // then
        assertThat(actual.urgent()).isFalse();
    }

    /** 마감일과 마감 시각이 모두 없으면 마감 없음과 긴급 아님을 반환하는지 검증한다. */
    @Test
    @DisplayName("마감이 없으면 마감 없음이며 긴급하지 않음")
    void calculatesNoDeadline() {
        // given
        DueLabeler labeler = labelerAt(LocalDateTime.parse("2026-11-11T12:00"));
        Task task = taskDue(null, null);

        // when
        DueLabeler.Due actual = labeler.calculate(task);

        // then
        assertThat(actual).isEqualTo(new DueLabeler.Due("마감 없음", false));
    }

    /** D02에 따라 남은 시간 차이의 초를 버린 분으로 문구와 긴급 여부를 검증한다. */
    @DisplayName("D02: 두 시각을 각각 절삭하지 않고 남은 시간의 초를 버림")
    @ParameterizedTest(name = "현재 {0}, 마감 {1}: {2}, 긴급={3}")
    @CsvSource({
        "12:00:00, 14:00:00, 14:00 마감, false",
        "12:00:00, 14:00:59, 14:00 마감, false",
        "12:00:59, 14:01:00, 14:01 마감, false",
        "12:00:01, 14:00:00, 14:00 마감 · 1시간 59분 남음, true",
        "12:00:59, 14:00:00, 14:00 마감 · 1시간 59분 남음, true",
        "12:00:59, 13:59:58, 13:59 마감 · 1시간 58분 남음, true",
        "12:00:59, 13:01:00, 13:01 마감 · 1시간 남음, true",
        "12:00:01, 13:00:00, 13:00 마감 · 59분 남음, true",
        "12:00:59, 12:02:00, 12:02 마감 · 1분 남음, true",
        "12:00:01, 12:01:00, 12:01 마감 · 지금 마감, true",
        "12:00:59, 12:00:59, 12:00 마감 · 지금 마감, true",
        "12:00:00, 12:00:59, 12:00 마감 · 지금 마감, true"
    })
    void truncatesRemainingDuration(String now, String target, String expectedLabel, boolean expectedUrgent) {
        // given
        LocalDate date = LocalDate.parse("2026-11-11");
        DueLabeler labeler = labelerAt(date.atTime(LocalTime.parse(now)));
        Task task = taskDue(date, LocalTime.parse(target));

        // when
        DueLabeler.Due actual = labeler.calculate(task);

        // then
        assertThat(actual).isEqualTo(new DueLabeler.Due(expectedLabel, expectedUrgent));
    }

    /** D03에 따라 일요일 당일에도 오늘 규칙의 기존 문구인 오늘 중을 우선하는지 검증한다. */
    @DisplayName("D03: 일요일 당일은 이번 주 마감보다 오늘 중을 우선하며 긴급하지 않음")
    @ParameterizedTest(name = "일요일 현재 시각 {0}")
    @CsvSource({"00:00:00", "12:00:00", "23:59:59"})
    void sundayDeadlineUsesTodayLabel(String now) {
        // given
        LocalDate sunday = LocalDate.parse("2026-11-15");
        DueLabeler labeler = labelerAt(sunday.atTime(LocalTime.parse(now)));
        Task task = taskDue(sunday, null);

        // when
        DueLabeler.Due actual = labeler.calculate(task);

        // then
        assertThat(actual).isEqualTo(new DueLabeler.Due("오늘 중", false));
    }

    /** 서울 로컬 시각을 고정된 순간으로 바꿔 시간대가 명시된 계산기를 만든다. */
    private static DueLabeler labelerAt(LocalDateTime now) {
        return new DueLabeler(Clock.fixed(now.atZone(SEOUL).toInstant(), SEOUL));
    }

    /** 영속성 컨텍스트 없이 마감 정보가 있는 테스트용 엔티티를 만든다. */
    private static Task taskDue(LocalDate date, LocalTime time) {
        return Task.create("마감 경계 검증", TaskStatus.TODO, TaskPriority.MEDIUM, null, date, time);
    }
}

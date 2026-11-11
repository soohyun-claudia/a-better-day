package com.abetterday.dashboard.service;

import com.abetterday.task.domain.TaskPriority;
import com.abetterday.task.domain.TaskStatus;
import com.abetterday.task.dto.TaskResponse;
import com.abetterday.task.repository.TaskRepository;
import com.abetterday.task.service.DueLabeler;
import com.abetterday.task.service.TaskService;
import java.time.*;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Spring·DB 없이 T-20 정렬·집계 계약과 D04 현재 동작을 검증한다. */
class DashboardServiceTest {
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-10T15:00:00Z"), SEOUL);
    private static final OffsetDateTime BASE = OffsetDateTime.now(CLOCK);
    private TaskService tasks;
    private DashboardService service;

    /** 외부 서비스는 목으로 대체하고 서울 고정 시각을 주입한다. */
    @BeforeEach
    void setUp() {
        tasks = mock(TaskService.class);
        service = new DashboardService(tasks, CLOCK);
    }

    /** 상태별 목록을 주입한다. */
    private void givenLists(List<TaskResponse> todos, List<TaskResponse> dones, List<TaskResponse> wishes) {
        when(tasks.getTasks(List.of(TaskStatus.TODO, TaskStatus.DOING))).thenReturn(todos);
        when(tasks.getDoneToday()).thenReturn(dones);
        when(tasks.getTasks(List.of(TaskStatus.WISH))).thenReturn(wishes);
    }

    /** 각 정렬 키에 필요한 값을 가진 응답을 만든다. */
    private TaskResponse item(long id, TaskStatus status, TaskPriority priority,
                              LocalDate date, LocalTime time, int createdOffset) {
        return new TaskResponse(id, "항목 " + id, status, priority, "업무", date, time,
                "전달할 문구", true, status == TaskStatus.DONE ? BASE.minusMinutes(id) : null,
                BASE.plusSeconds(createdOffset), BASE);
    }

    /** 기본 응답 목록을 만든다. */
    private List<TaskResponse> items(int count, TaskStatus status) {
        return IntStream.range(0, count).mapToObj(i ->
                item(i + 1L, status, TaskPriority.MEDIUM, null, null, i)).toList();
    }

    /** DOING 우선과 우선순위가 마감·생성 시각보다 먼저 적용되는지 검증한다. */
    @Test
    @DisplayName("DOING → HIGH/MEDIUM/LOW 정렬 우선순위")
    void sortStatusAndPriority() {
        // given
        var date = LocalDate.of(2026, 11, 11);
        var low = item(4, TaskStatus.TODO, TaskPriority.LOW, date.minusDays(1), null, -100);
        var medium = item(3, TaskStatus.TODO, TaskPriority.MEDIUM, date, null, -10);
        var high = item(2, TaskStatus.TODO, TaskPriority.HIGH, null, null, 100);
        var doing = item(1, TaskStatus.DOING, TaskPriority.LOW, null, null, 200);
        givenLists(List.of(low, medium, high, doing), List.of(), List.of());
        // when
        var response = service.getDashboard();
        // then
        assertThat(response.todayTodo().items()).containsExactly(doing, high, medium, low);
        assertThat(response.topBar().doingTask()).isSameAs(doing);
        assertThat(response.todayTodo().items().getFirst().dueLabel()).isEqualTo("전달할 문구");
        assertThat(response.todayTodo().items().getFirst().urgent()).isTrue();
    }

    /** 날짜·시각·생성일의 순서와 마감 없음의 후순위를 검증한다. */
    @Test
    @DisplayName("마감 날짜 → 시각 → 생성 시각, 마감 없음은 뒤")
    void sortDueAndCreatedAt() {
        // given
        var date = LocalDate.of(2026, 11, 11);
        var early = item(1, TaskStatus.TODO, TaskPriority.HIGH, date, LocalTime.of(9, 0), 100);
        var old = item(2, TaskStatus.TODO, TaskPriority.HIGH, date, LocalTime.NOON, 0);
        var recent = item(3, TaskStatus.TODO, TaskPriority.HIGH, date, LocalTime.NOON, 1);
        var tomorrow = item(4, TaskStatus.TODO, TaskPriority.HIGH, date.plusDays(1), LocalTime.MIDNIGHT, -50);
        var noDue = item(5, TaskStatus.TODO, TaskPriority.HIGH, null, null, -100);
        givenLists(List.of(noDue, tomorrow, recent, old, early), List.of(), List.of());
        // when / then
        assertThat(service.getDashboard().todayTodo().items()).containsExactly(early, old, recent, tomorrow, noDue);
    }

    /** 동일 키에서는 ID 정렬을 요구하지 않고 입력 순서가 유지되는지 관찰한다. */
    @Test
    @DisplayName("완전 동률은 서비스 입력 순서 유지: DB 순서는 보장하지 않음")
    void stableTie() {
        // given
        var first = item(9, TaskStatus.TODO, TaskPriority.MEDIUM, null, null, 0);
        var second = item(1, TaskStatus.TODO, TaskPriority.MEDIUM, null, null, 0);
        var wishFirst = item(9, TaskStatus.WISH, TaskPriority.MEDIUM, null, null, 0);
        var wishSecond = item(1, TaskStatus.WISH, TaskPriority.MEDIUM, null, null, 0);
        givenLists(List.of(first, second), List.of(), List.of(wishFirst, wishSecond));
        // when
        var response = service.getDashboard();
        // then
        assertThat(response.todayTodo().items()).containsExactly(first, second);
        assertThat(response.wish().items()).containsExactly(wishFirst, wishSecond);
    }

    /** 표시 제한 전 전체수를 집계하고 모든 패널 경계를 확인한다. */
    @DisplayName("0/4/5/7/8개 표시 제한과 전체·숨김·완료 수")
    @ParameterizedTest
    @ValueSource(ints = {0, 4, 5, 7, 8})
    void panelBoundaries(int count) {
        // given
        var todos = items(count, TaskStatus.TODO);
        var dones = items(count, TaskStatus.DONE);
        var wishes = items(count, TaskStatus.WISH).reversed();
        givenLists(todos, dones, wishes);
        // when
        var response = service.getDashboard();
        // then
        assertThat(response.todayTodo().items()).containsExactlyElementsOf(todos.subList(0, Math.min(7, count)));
        assertThat(response.todayTodo().totalCount()).isEqualTo(count);
        assertThat(response.todayTodo().hiddenCount()).isEqualTo(Math.max(0, count - 7));
        assertThat(response.wish().items()).containsExactlyElementsOf(items(count, TaskStatus.WISH)
                .subList(0, Math.min(4, count)));
        assertThat(response.wish().totalCount()).isEqualTo(count);
        assertThat(response.wish().hiddenCount()).isEqualTo(Math.max(0, count - 4));
        assertThat(response.done().items()).containsExactlyElementsOf(dones.subList(0, Math.min(4, count)));
        assertThat(response.done().doneCount()).isEqualTo(count);
        assertThat(response.topBar().progress().done()).isEqualTo(count);
        assertThat(response.topBar().progress().total()).isEqualTo(count * 2);
        assertThat(response.topBar().doingTask()).isNull();
        verify(tasks).getTasks(List.of(TaskStatus.TODO, TaskStatus.DOING));
        verify(tasks).getTasks(List.of(TaskStatus.WISH));
        verify(tasks).getDoneToday();
        verifyNoMoreInteractions(tasks);
    }

    /** 표시수 대신 전체수로 정수 경계 진행률을 계산한다. */
    @DisplayName("진행률 0/50/100%와 분모 0, WISH 제외")
    @ParameterizedTest
    @CsvSource({"0,0,0", "8,0,0", "8,8,50", "0,8,100"})
    void progress(int remaining, int done, int percent) {
        // given
        givenLists(items(remaining, TaskStatus.TODO), items(done, TaskStatus.DONE), items(5, TaskStatus.WISH));
        // when
        var result = service.getDashboard().topBar().progress();
        // then
        assertThat(result.done()).isEqualTo(done);
        assertThat(result.total()).isEqualTo(remaining + done);
        assertThat(result.percent()).isEqualTo(percent);
    }

    /** 서울 자정 양쪽과 연도 경계에서 날짜·요일을 검증한다. */
    @DisplayName("today와 dateLabel은 서울 날짜·요일")
    @ParameterizedTest
    @CsvSource({"2026-11-10T14:59:59Z,2026-11-10,2026년 11월 10일 (화)",
            "2026-11-10T15:00:00Z,2026-11-11,2026년 11월 11일 (수)",
            "2026-12-31T15:00:00Z,2027-01-01,2027년 1월 1일 (금)"})
    void seoulDate(Instant instant, String today, String label) {
        // given
        service = new DashboardService(tasks, Clock.fixed(instant, SEOUL));
        givenLists(List.of(), List.of(), List.of());
        // when
        var bar = service.getDashboard().topBar();
        // then
        assertThat(bar.today()).isEqualTo(today);
        assertThat(bar.dateLabel()).isEqualTo(label);
        assertThat(bar.doingTask()).isNull();
    }

    /** D04의 날짜만 있는 마감 순서는 현재 동작만 기록한다. */
    @Test
    @DisplayName("D04 관찰: 같은 날짜에서는 시각 있는 마감이 먼저")
    void observeUntimedDuePolicy() {
        // given
        var date = LocalDate.of(2026, 11, 11);
        var untimed = item(1, TaskStatus.TODO, TaskPriority.HIGH, date, null, -100);
        var timed = item(2, TaskStatus.TODO, TaskPriority.HIGH, date, LocalTime.of(23, 59), 100);
        givenLists(List.of(untimed, timed), List.of(), List.of());
        // when / then
        assertThat(service.getDashboard().todayTodo().items()).containsExactly(timed, untimed);
    }

    /** D04의 소수 퍼센트는 현재 반올림 결과만 기록한다. */
    @DisplayName("D04 관찰: 소수 퍼센트의 현재 반올림")
    @ParameterizedTest
    @CsvSource({"2,1,33", "1,2,67", "7,1,13"})
    void observeFractionPolicy(int remaining, int done, int observed) {
        // given
        givenLists(items(remaining, TaskStatus.TODO), items(done, TaskStatus.DONE), List.of());
        // when / then
        assertThat(service.getDashboard().topBar().progress().percent()).isEqualTo(observed);
    }

    /** 고정 Clock에서는 실제 완료 조회 구간과 상단 날짜가 일치한다. */
    @DisplayName("고정 Clock의 자정 전후 완료 조회 구간과 today 일치")
    @ParameterizedTest
    @CsvSource({"2026-11-10T14:59:59Z,2026-11-10", "2026-11-10T15:00:00Z,2026-11-11"})
    void fixedRequestConsistency(Instant instant, LocalDate expected) {
        // given
        Clock fixed = Clock.fixed(instant, SEOUL);
        TaskRepository repository = mock(TaskRepository.class);
        when(repository.findByStatusIn(anyList())).thenReturn(List.of());
        when(repository.findDoneToday(any(), any())).thenReturn(List.of());
        service = new DashboardService(new TaskService(repository, mock(DueLabeler.class), fixed), fixed);
        // when
        var response = service.getDashboard();
        // then
        var start = expected.atStartOfDay(SEOUL).toOffsetDateTime();
        verify(repository).findDoneToday(start, start.plusDays(1));
        assertThat(response.topBar().today()).isEqualTo(expected.toString());
    }

    /** 실제 TaskService와 목 Repository로 자정 걸침의 현재 날짜 불일치를 재현한다. */
    @Test
    @DisplayName("D04 관찰: 완료 조회는 전날, 상단 날짜는 다음 날이 될 수 있음")
    void observeCrossMidnightRequest() {
        // given
        Clock crossing = mock(Clock.class);
        when(crossing.getZone()).thenReturn(SEOUL);
        when(crossing.instant()).thenReturn(Instant.parse("2026-11-10T14:59:59Z"),
                Instant.parse("2026-11-10T15:00:00Z"));
        TaskRepository repository = mock(TaskRepository.class);
        when(repository.findByStatusIn(anyList())).thenReturn(List.of());
        when(repository.findDoneToday(any(), any())).thenReturn(List.of());
        var actualTasks = new TaskService(repository, mock(DueLabeler.class), crossing);
        service = new DashboardService(actualTasks, crossing);
        // when
        var response = service.getDashboard();
        // then
        verify(repository).findDoneToday(OffsetDateTime.parse("2026-11-10T00:00:00+09:00"),
                OffsetDateTime.parse("2026-11-11T00:00:00+09:00"));
        assertThat(response.topBar().today()).isEqualTo("2026-11-11");
        verify(repository).findByStatusIn(List.of(TaskStatus.TODO, TaskStatus.DOING));
        verify(repository).findByStatusIn(List.of(TaskStatus.WISH));
        verifyNoMoreInteractions(repository);
    }
}

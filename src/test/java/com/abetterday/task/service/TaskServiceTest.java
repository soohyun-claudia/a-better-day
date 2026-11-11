package com.abetterday.task.service;

import com.abetterday.common.exception.BusinessException;
import com.abetterday.task.domain.Task;
import com.abetterday.task.domain.TaskPriority;
import com.abetterday.task.domain.TaskStatus;
import com.abetterday.task.dto.CreateTaskRequest;
import com.abetterday.task.dto.TaskResponse;
import com.abetterday.task.repository.TaskRepository;
import java.time.*;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.InOrder;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Spring과 DB 없이 T-18 생성·전환 계약을 검증한다. */
class TaskServiceTest {
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-10T15:00:00Z"), SEOUL);
    private TaskRepository repository;
    private DueLabeler labeler;
    private TaskService service;

    /** 매 테스트마다 외부 의존성을 새 목으로 격리한다. */
    @BeforeEach
    void setUp() {
        repository = mock(TaskRepository.class);
        labeler = mock(DueLabeler.class);
        service = new TaskService(repository, labeler, CLOCK);
    }

    /** 저장 결과에 영속성 생성 필드를 채워 응답 매핑을 검증한다. */
    private void stubSave() {
        when(repository.saveAndFlush(any(Task.class))).thenAnswer(call -> {
            Task task = call.getArgument(0);
            ReflectionTestUtils.setField(task, "id", 7L);
            ReflectionTestUtils.setField(task, "createdAt", OffsetDateTime.now(CLOCK).minusDays(1));
            ReflectionTestUtils.setField(task, "updatedAt", OffsetDateTime.now(CLOCK));
            return task;
        });
        when(labeler.calculate(any(Task.class))).thenReturn(new DueLabeler.Due("검증 문구", true));
    }

    /** WISH와 TODO 생성 기본값 및 공백 정규화를 확인한다. */
    @DisplayName("WISH/TODO 생성 기본값과 제목·빈 카테고리 정규화")
    @ParameterizedTest
    @EnumSource(value = TaskStatus.class, names = {"WISH", "TODO"})
    void createDefaults(TaskStatus status) {
        // given
        stubSave();
        var request = new CreateTaskRequest("  제목  ", status, null, "  ", null, null);
        // when
        TaskResponse response = service.createTask(request);
        // then
        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.title()).isEqualTo("제목");
        assertThat(response.status()).isEqualTo(status);
        assertThat(response.priority()).isEqualTo(TaskPriority.MEDIUM);
        assertThat(response.category()).isNull();
        assertThat(response.dueDate()).isNull();
        assertThat(response.dueTime()).isNull();
        assertThat(response.doneAt()).isNull();
        assertThat(response.createdAt()).isEqualTo(OffsetDateTime.now(CLOCK).minusDays(1));
        assertThat(response.updatedAt()).isEqualTo(OffsetDateTime.now(CLOCK));
        assertThat(response.dueLabel()).isEqualTo("검증 문구");
        assertThat(response.urgent()).isTrue();
        verify(repository).saveAndFlush(argThat(task -> !task.isDeleted()));
        verifyNoMoreInteractions(repository);
    }

    /** 날짜만 또는 날짜와 시각을 가진 명시적 생성 필드를 보존한다. */
    @DisplayName("명시 우선순위·카테고리·마감 보존")
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void createExplicitFields(boolean timed) {
        // given
        stubSave();
        LocalDate date = LocalDate.of(2026, 11, 1);
        LocalTime time = timed ? LocalTime.MIDNIGHT : null;
        // when
        var response = service.createTask(new CreateTaskRequest("가".repeat(200), TaskStatus.WISH,
                TaskPriority.HIGH, "  업무  ", date, time));
        // then
        assertThat(response.title()).hasSize(200);
        assertThat(response.priority()).isEqualTo(TaskPriority.HIGH);
        assertThat(response.category()).isEqualTo("업무");
        assertThat(response.dueDate()).isEqualTo(date);
        assertThat(response.dueTime()).isEqualTo(time);
    }

    /** 생성 시 금지 상태와 null 상태를 저장 전에 거부한다. */
    @DisplayName("DOING/DONE/null 생성은 400이며 저장하지 않음")
    @ParameterizedTest
    @NullSource
    @EnumSource(value = TaskStatus.class, names = {"DOING", "DONE"})
    void rejectCreateStatus(TaskStatus status) {
        // given
        var request = new CreateTaskRequest("제목", status, null, null, null, null);
        // when / then
        assertBusiness(() -> service.createTask(request), "INVALID_CREATE_STATUS", HttpStatus.BAD_REQUEST);
        verifyNoInteractions(repository, labeler);
    }

    /** 날짜 없는 자정 시각도 마감 검증에서 거부한다. */
    @DisplayName("날짜 없는 시각은 400이며 저장하지 않음")
    @ParameterizedTest
    @EnumSource(value = TaskStatus.class, names = {"WISH", "TODO"})
    void rejectTimeWithoutDate(TaskStatus status) {
        // given
        var request = new CreateTaskRequest("제목", status, null, null, null, LocalTime.MIDNIGHT);
        // when / then
        assertBusiness(() -> service.createTask(request), "DUE_TIME_WITHOUT_DATE", HttpStatus.BAD_REQUEST);
        verifyNoInteractions(repository, labeler);
    }

    /** 전체 상태 조합을 제공한다. */
    static Stream<Arguments> transitions() {
        return Stream.of(TaskStatus.values()).flatMap(from ->
                Stream.of(TaskStatus.values()).map(to -> Arguments.of(from, to)));
    }

    /** 모든 허용·금지 전환과 필드 보존 및 부수 조회를 검증한다. */
    @DisplayName("4×4 상태 전환표 및 금지 전환의 무변경 보장")
    @ParameterizedTest(name = "{0} → {1}")
    @MethodSource("transitions")
    void transitionMatrix(TaskStatus from, TaskStatus to) {
        // given
        Task task = task(from);
        var oldDoneAt = task.getDoneAt();
        when(repository.findById(7L)).thenReturn(Optional.of(task));
        boolean allowed = (from == TaskStatus.TODO && (to == TaskStatus.DOING || to == TaskStatus.DONE))
                || (from == TaskStatus.DOING && to == TaskStatus.DONE)
                || (from == TaskStatus.WISH && to == TaskStatus.TODO);
        // when / then
        if (!allowed) {
            assertBusiness(() -> service.changeStatus(7L, to), "INVALID_STATUS_TRANSITION", HttpStatus.CONFLICT);
            assertThat(task.getStatus()).isEqualTo(from);
            assertThat(task.getDoneAt()).isEqualTo(oldDoneAt);
            assertThat(task.getPriority()).isEqualTo(TaskPriority.HIGH);
            assertThat(task.getDueDate()).isEqualTo(LocalDate.of(2026, 12, 1));
            assertThat(task.getDueTime()).isEqualTo(LocalTime.NOON);
            verify(repository).findById(7L);
            verifyNoMoreInteractions(repository);
            verifyNoInteractions(labeler);
            return;
        }
        stubSave();
        var response = service.changeStatus(7L, to);
        assertThat(response.status()).isEqualTo(to);
        assertThat(response.title()).isEqualTo("제목");
        assertThat(response.category()).isEqualTo("업무");
        assertThat(response.doneAt()).isEqualTo(to == TaskStatus.DONE ? OffsetDateTime.now(CLOCK) : null);
        assertThat(response.dueDate()).isEqualTo(to == TaskStatus.TODO
                ? LocalDate.of(2026, 11, 15) : LocalDate.of(2026, 12, 1));
        assertThat(response.priority()).isEqualTo(to == TaskStatus.TODO ? TaskPriority.MEDIUM : TaskPriority.HIGH);
        assertThat(response.dueTime()).isEqualTo(to == TaskStatus.TODO ? null : LocalTime.NOON);
        verify(repository).findById(7L);
        if (to == TaskStatus.DOING) {
            verify(repository).findFirstByStatus(TaskStatus.DOING);
        }
        verify(repository).saveAndFlush(task);
        verifyNoMoreInteractions(repository);
    }

    /** 기존 DOING의 flush 시점에 대상이 아직 TODO인지 확인한다. */
    @Test
    @DisplayName("기존 DOING을 TODO로 flush한 이후에만 대상을 DOING으로 변경")
    void flushOldDoingBeforeStartingTarget() {
        // given
        Task target = task(TaskStatus.TODO);
        Task previous = task(TaskStatus.DOING);
        when(repository.findById(7L)).thenReturn(Optional.of(target));
        when(repository.findFirstByStatus(TaskStatus.DOING)).thenReturn(Optional.of(previous));
        when(repository.saveAndFlush(previous)).thenAnswer(call -> {
            assertThat(previous.getStatus()).isEqualTo(TaskStatus.TODO);
            assertThat(previous.getDoneAt()).isNull();
            assertThat(target.getStatus()).isEqualTo(TaskStatus.TODO);
            return previous;
        });
        when(repository.saveAndFlush(target)).thenAnswer(call -> {
            assertThat(previous.getStatus()).isEqualTo(TaskStatus.TODO);
            assertThat(target.getStatus()).isEqualTo(TaskStatus.DOING);
            return target;
        });
        when(labeler.calculate(target)).thenReturn(new DueLabeler.Due("", false));
        // when
        service.changeStatus(7L, TaskStatus.DOING);
        // then
        InOrder order = inOrder(repository);
        order.verify(repository).findById(7L);
        order.verify(repository).findFirstByStatus(TaskStatus.DOING);
        order.verify(repository).saveAndFlush(previous);
        order.verify(repository).saveAndFlush(target);
        order.verifyNoMoreInteractions();
        assertThat(previous.getPriority()).isEqualTo(TaskPriority.HIGH);
        assertThat(previous.getDueTime()).isEqualTo(LocalTime.NOON);
    }

    /** 없는 ID는 상태 조회나 저장 없이 404를 반환한다. */
    @DisplayName("없는 ID는 모든 요청 상태에서 404")
    @ParameterizedTest
    @EnumSource(TaskStatus.class)
    void missingTask(TaskStatus status) {
        // given
        when(repository.findById(404L)).thenReturn(Optional.empty());
        // when / then
        assertBusiness(() -> service.changeStatus(404L, status), "TASK_NOT_FOUND", HttpStatus.NOT_FOUND);
        verify(repository).findById(404L);
        verifyNoMoreInteractions(repository);
        verifyNoInteractions(labeler);
    }

    /** 서울 일요일 자정 양쪽에서 승격 기준일을 검증한다. */
    @DisplayName("승격은 서울 일요일 당일을 포함하며 월요일에는 다음 일요일")
    @ParameterizedTest
    @CsvSource({"2026-11-14T14:59:59Z,2026-11-15", "2026-11-14T15:00:00Z,2026-11-15",
            "2026-11-15T14:59:59Z,2026-11-15", "2026-11-15T15:00:00Z,2026-11-22"})
    void promotionBoundary(Instant instant, LocalDate expected) {
        // given
        service = new TaskService(repository, labeler, Clock.fixed(instant, SEOUL));
        Task task = task(TaskStatus.WISH);
        when(repository.findById(7L)).thenReturn(Optional.of(task));
        stubSave();
        // when
        var response = service.changeStatus(7L, TaskStatus.TODO);
        // then
        assertThat(response.dueDate()).isEqualTo(expected);
        assertThat(response.dueTime()).isNull();
        assertThat(response.priority()).isEqualTo(TaskPriority.MEDIUM);
    }

    /** 기존 DOING flush 실패 시 대상 변경과 응답 생성을 시작하지 않는다. */
    @Test
    @DisplayName("기존 DOING flush 실패 시 대상은 TODO를 유지")
    void stopOnPreviousFlushFailure() {
        // given
        Task target = task(TaskStatus.TODO);
        Task previous = task(TaskStatus.DOING);
        var failure = new IllegalStateException("저장 실패");
        when(repository.findById(7L)).thenReturn(Optional.of(target));
        when(repository.findFirstByStatus(TaskStatus.DOING)).thenReturn(Optional.of(previous));
        when(repository.saveAndFlush(previous)).thenThrow(failure);
        // when / then
        assertThatThrownBy(() -> service.changeStatus(7L, TaskStatus.DOING)).isSameAs(failure);
        assertThat(target.getStatus()).isEqualTo(TaskStatus.TODO);
        verify(repository, never()).saveAndFlush(target);
        verifyNoInteractions(labeler);
    }

    /** 비완료 도메인 전환은 방어적으로 남아 있는 완료 시각을 지운다. */
    @DisplayName("비완료 도메인 메서드는 doneAt 초기화")
    @ParameterizedTest
    @EnumSource(value = TaskStatus.class, names = {"TODO", "DOING", "WISH"})
    void clearStaleDoneAt(TaskStatus target) {
        // given
        Task task = task(TaskStatus.DONE);
        // when
        switch (target) {
            case TODO -> task.markTodo();
            case DOING -> task.markDoing();
            case WISH -> task.promote(LocalDate.of(2026, 11, 15));
            default -> throw new AssertionError("테스트 대상 오류");
        }
        // then
        assertThat(task.getDoneAt()).isNull();
        assertThat(task.getStatus()).isEqualTo(target == TaskStatus.WISH ? TaskStatus.TODO : target);
    }

    /** 쓰기 메서드가 읽기 전용 트랜잭션을 재정의하는지 정적으로 확인한다. */
    @Test
    @DisplayName("생성과 상태 변경의 쓰기 트랜잭션 선언")
    void writeTransactionDeclarations() throws NoSuchMethodException {
        // given / when / then
        assertThat(TaskService.class.getAnnotation(Transactional.class).readOnly()).isTrue();
        assertThat(TaskService.class.getMethod("createTask", CreateTaskRequest.class)
                .getAnnotation(Transactional.class).readOnly()).isFalse();
        assertThat(TaskService.class.getMethod("changeStatus", Long.class, TaskStatus.class)
                .getAnnotation(Transactional.class).readOnly()).isFalse();
    }

    /** 전환 전 상태에 맞는 엔티티를 만든다. */
    private Task task(TaskStatus status) {
        Task task = Task.create("제목", status, TaskPriority.HIGH, "업무",
                LocalDate.of(2026, 12, 1), LocalTime.NOON);
        if (status == TaskStatus.DONE) {
            task.markDone(OffsetDateTime.now(CLOCK).minusDays(1));
        }
        return task;
    }

    /** 비즈니스 예외의 코드와 HTTP 상태를 함께 검증한다. */
    private void assertBusiness(org.assertj.core.api.ThrowableAssert.ThrowingCallable action,
                                String code, HttpStatus status) {
        assertThatThrownBy(action).isInstanceOfSatisfying(BusinessException.class, exception -> {
            assertThat(exception.getCode()).isEqualTo(code);
            assertThat(exception.getHttpStatus()).isEqualTo(status);
            assertThat(exception.getMessage()).isNotBlank();
        });
    }
}

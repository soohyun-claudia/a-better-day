package com.abetterday;

import com.abetterday.common.exception.BusinessException;
import com.abetterday.common.exception.GlobalExceptionHandler;
import com.abetterday.dashboard.controller.DashboardController;
import com.abetterday.dashboard.service.DashboardService;
import com.abetterday.task.controller.TaskController;
import com.abetterday.task.domain.TaskStatus;
import com.abetterday.task.service.TaskService;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mockingDetails;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** DB 없이 PH3-HTTP-01의 상태·본문·헤더·로그 수준과 기존 예외 처리의 회귀를 검증한다. */
@WebMvcTest(controllers = {TaskController.class, DashboardController.class})
class Phase3HttpErrorReviewTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ApplicationContext context;

    private Logger adviceLogger;
    private Level originalLevel;
    private ListAppender<ILoggingEvent> logEvents;

    /** 테스트 동안 Advice의 실제 로그 이벤트를 debug 수준부터 수집한다. */
    @BeforeEach
    void captureAdviceLogs() {
        adviceLogger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        originalLevel = adviceLogger.getLevel();
        logEvents = new ListAppender<>();
        logEvents.start();
        adviceLogger.addAppender(logEvents);
        adviceLogger.setLevel(Level.DEBUG);
        assertThat(context.getBeanNamesForType(DataSource.class)).isEmpty();
        assertThat(mockingDetails(taskService).isMock()).isTrue();
        assertThat(mockingDetails(dashboardService).isMock()).isTrue();
    }

    /** 로그 수집기를 제거하고 원래 로거 설정을 복원한다. */
    @AfterEach
    void restoreAdviceLogger() {
        adviceLogger.detachAppender(logEvents);
        logEvents.stop();
        adviceLogger.setLevel(originalLevel);
    }

    @MockitoBean
    private TaskService taskService;

    @MockitoBean
    private DashboardService dashboardService;

    /** Phase 3 요청과 추가 요청의 실제 예외 유형을 제공한다. */
    static Stream<Arguments> routingFailures() {
        return Stream.of(
                Arguments.of("GET", "/favicon.ico", NoResourceFoundException.class),
                Arguments.of("GET", "/api/none", NoResourceFoundException.class),
                Arguments.of("GET", "/nothing", NoResourceFoundException.class),
                Arguments.of("POST", "/api/none", NoResourceFoundException.class),
                Arguments.of("GET", "/api/dashboard/x", NoResourceFoundException.class),
                Arguments.of("PUT", "/api/tasks/19", HttpRequestMethodNotSupportedException.class),
                Arguments.of("GET", "/api/tasks/99999", HttpRequestMethodNotSupportedException.class),
                Arguments.of("DELETE", "/api/tasks/99999", HttpRequestMethodNotSupportedException.class),
                Arguments.of("PATCH", "/api/tasks/19", HttpMediaTypeNotSupportedException.class));
    }

    /** 표준 예외의 상태·오류 계약·로그 수준과 응답 헤더를 검증한다. */
    @ParameterizedTest
    @MethodSource("routingFailures")
    @DisplayName("표준 MVC 예외는 404·405·415 및 지정 코드로 처리된다")
    void preservesRoutingErrorContract(String method, String path, Class<? extends Exception> exceptionType)
            throws Exception {
        // given
        var request = MockMvcRequestBuilders.request(HttpMethod.valueOf(method), path);
        if (method.equals("PATCH")) {
            request.contentType("text/plain").content("{\"status\":\"DONE\"}");
        }
        int expectedStatus = exceptionType == NoResourceFoundException.class ? 404
                : exceptionType == HttpRequestMethodNotSupportedException.class ? 405 : 415;
        String expectedCode = switch (expectedStatus) {
            case 404 -> "RESOURCE_NOT_FOUND";
            case 405 -> "METHOD_NOT_ALLOWED";
            default -> "UNSUPPORTED_MEDIA_TYPE";
        };
        String expectedMessage = switch (expectedStatus) {
            case 404 -> "요청한 리소스를 찾을 수 없습니다.";
            case 405 -> "지원하지 않는 요청 방식입니다.";
            default -> "지원하지 않는 콘텐츠 형식입니다.";
        };

        // when
        var result = mvc.perform(request)
                .andExpect(status().is(expectedStatus))
                .andExpect(jsonPath("$.code").value(expectedCode))
                .andExpect(jsonPath("$.message").value(expectedMessage))
                .andExpect(jsonPath("$.length()").value(2))
                .andReturn();

        // then
        assertThat(result.getResolvedException()).isExactlyInstanceOf(exceptionType);
        assertAdviceLogLevel(path.equals("/favicon.ico") ? Level.DEBUG : Level.WARN);
        if (result.getResolvedException() instanceof HttpRequestMethodNotSupportedException failure) {
            assertThat(result.getResponse().getHeaders("Allow"))
                    .containsExactlyElementsOf(failure.getHeaders().get("Allow"));
            assertThat(result.getResponse().getHeader("Allow")).contains("PATCH");
        }
        if (result.getResolvedException() instanceof HttpMediaTypeNotSupportedException failure) {
            failure.getHeaders().forEach((name, values) ->
                    assertThat(result.getResponse().getHeaders(name)).containsExactlyElementsOf(values));
        }
        verifyNoInteractions(taskService, dashboardService);
    }

    /** 정적 리소스 매핑이 없는 별도 구성에서 NoHandlerFoundException 경로를 구분한다. */
    @Test
    @DisplayName("보조 구성: NoHandlerFoundException도 404와 한국어 오류 본문을 반환한다")
    void preservesNoHandlerFailure() throws Exception {
        // given
        MockMvc isolated = MockMvcBuilders.standaloneSetup(new TaskController(taskService))
                .setControllerAdvice(new GlobalExceptionHandler()).build();

        // when
        var result = isolated.perform(get("/api/none"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("요청한 리소스를 찾을 수 없습니다."))
                .andExpect(jsonPath("$.length()").value(2)).andReturn();

        // then
        assertThat(result.getResolvedException()).isExactlyInstanceOf(NoHandlerFoundException.class);
        assertAdviceLogLevel(Level.WARN);
        verifyNoInteractions(taskService);
    }

    /** 포괄 핸들러를 제외한 MVC 기본 처리의 404·405·415와 Allow 헤더를 확인한다. */
    @Test
    @DisplayName("대조 구성: MVC 기본 처리는 404·405·415를 보존한다")
    void checksDefaultMvcStatuses() throws Exception {
        // given
        MockMvc isolated = MockMvcBuilders.standaloneSetup(new TaskController(taskService)).build();

        // when
        isolated.perform(get("/api/none")).andExpect(status().isNotFound());
        var methodResult = isolated.perform(get("/api/tasks/99999"))
                .andExpect(status().isMethodNotAllowed()).andReturn();
        isolated.perform(patch("/api/tasks/19").contentType("text/plain").content("{}"))
                .andExpect(status().isUnsupportedMediaType());

        // then
        assertThat(methodResult.getResponse().getHeader("Allow")).contains("PATCH");
        verifyNoInteractions(taskService);
    }

    /** 기존 입력 오류의 상태와 오류 코드를 제공한다. */
    static Stream<Arguments> badRequests() {
        return Stream.of(
                Arguments.of("/api/tasks/19", "{", "BAD_REQUEST"),
                Arguments.of("/api/tasks/19", "{\"status\":\"UNKNOWN\"}", "BAD_REQUEST"),
                Arguments.of("/api/tasks/19", "{}", "VALIDATION_ERROR"),
                Arguments.of("/api/tasks/invalid", "{\"status\":\"DONE\"}", "BAD_REQUEST"));
    }

    /** 기존 파싱·검증·타입 오류의 400 처리를 확인한다. */
    @ParameterizedTest
    @MethodSource("badRequests")
    @DisplayName("기존 400 오류 처리는 유지된다")
    void preservesBadRequests(String path, String body, String code) throws Exception {
        // given / when
        mvc.perform(patch(path).contentType("application/json").content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.length()").value(2));

        // then
        verifyNoInteractions(taskService);
        assertAdviceLogLevel(Level.WARN);
    }

    /** 서비스에서 발생하는 비즈니스 예외와 예상하지 못한 예외를 제공한다. */
    static Stream<Arguments> serviceFailures() {
        return Stream.of(
                Arguments.of(new BusinessException("INVALID_CREATE_STATUS", "생성 상태 오류", HttpStatus.BAD_REQUEST),
                        400, "INVALID_CREATE_STATUS"),
                Arguments.of(new BusinessException("TASK_NOT_FOUND", "해당 할 일을 찾을 수 없어요.", HttpStatus.NOT_FOUND),
                        404, "TASK_NOT_FOUND"),
                Arguments.of(new BusinessException("INVALID_STATUS_TRANSITION", "지금 상태에서는 바꿀 수 없어요.",
                        HttpStatus.CONFLICT), 409, "INVALID_STATUS_TRANSITION"),
                Arguments.of(new IllegalStateException("테스트 내부 오류"), 500, "INTERNAL_ERROR"));
    }

    /** 서비스 모의 예외에 대한 기존 400·404·409·500 응답 계약을 확인한다. */
    @ParameterizedTest
    @MethodSource("serviceFailures")
    @DisplayName("기존 비즈니스 예외와 예상하지 못한 오류의 변환을 유지한다")
    void preservesServiceFailures(RuntimeException failure, int expectedStatus, String code) throws Exception {
        // given
        when(taskService.changeStatus(99999L, TaskStatus.DONE)).thenThrow(failure);

        // when / then
        var result = mvc.perform(patch("/api/tasks/99999").contentType("application/json")
                        .content("{\"status\":\"DONE\"}"))
                .andExpect(status().is(expectedStatus)).andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.message").value(expectedStatus == 500
                        ? "서버 오류가 발생했습니다." : failure.getMessage()))
                .andExpect(jsonPath("$.length()").value(2)).andReturn();
        assertAdviceLogLevel(expectedStatus == 500 ? Level.ERROR : Level.WARN);
        if (expectedStatus == 500) {
            assertThat(result.getResponse().getContentAsString())
                    .doesNotContain(failure.getMessage(), "IllegalStateException", "stackTrace", "at com.abetterday");
            assertThat(logEvents.list.getFirst().getThrowableProxy().getMessage()).isEqualTo(failure.getMessage());
        }
    }

    /** MVC 테스트에 DB 연결 빈이 없고 서비스가 모의 객체인지 검증한다. */
    @Test
    @DisplayName("MVC 테스트는 DataSource 없이 모의 서비스만 사용한다")
    void verifiesDatabaseIsolation() {
        // given / when / then
        assertThat(context.getBeanNamesForType(DataSource.class)).isEmpty();
        assertThat(context.containsBean("entityManagerFactory")).isFalse();
        assertThat(context.containsBean("flyway")).isFalse();
        assertThat(mockingDetails(taskService).isMock()).isTrue();
        assertThat(mockingDetails(dashboardService).isMock()).isTrue();
    }

    /** 잘못된 status 쿼리 값의 기존 BAD_REQUEST 처리를 검증한다. */
    @Test
    @DisplayName("잘못된 status 쿼리는 400 BAD_REQUEST와 warn 로그를 유지한다")
    void preservesInvalidStatusQuery() throws Exception {
        // given / when
        mvc.perform(get("/api/tasks").param("status", "INVALID"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.length()").value(2));

        // then
        assertAdviceLogLevel(Level.WARN);
        verifyNoInteractions(taskService);
    }

    /** 정상 정적 자산과 목록 조회가 새 핸들러의 영향을 받지 않는지 확인한다. */
    @Test
    @DisplayName("정상 정적 자산과 목록 조회는 200을 유지한다")
    void preservesSuccessfulRequests() throws Exception {
        // given
        when(taskService.getTasks(null)).thenReturn(java.util.List.of());

        // when
        mvc.perform(get("/vendor/bootstrap-5.3.8/css/bootstrap.min.css")).andExpect(status().isOk());
        mvc.perform(get("/api/tasks")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // then
        assertThat(logEvents.list).isEmpty();
    }

    /** Advice가 정확히 한 건의 지정 수준 로그만 출력했는지 확인한다. */
    private void assertAdviceLogLevel(Level expectedLevel) {
        assertThat(logEvents.list).hasSize(1);
        assertThat(logEvents.list.getFirst().getLevel()).isEqualTo(expectedLevel);
    }
}

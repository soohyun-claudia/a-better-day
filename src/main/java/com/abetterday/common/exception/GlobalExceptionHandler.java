package com.abetterday.common.exception;

import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** 모든 컨트롤러의 예외를 한 곳에서 변환해 {"code","message"} 형식으로 응답한다. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 브라우저가 자동 요청하는 favicon의 리소스 경로 */
    private static final String FAVICON_PATH = "favicon.ico";

    /** 비즈니스 규칙 위반·검증 오류(400/404/409)를 지정된 상태 코드로 변환한다. */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        log.warn("비즈니스 예외: code={}, message={}", e.getCode(), e.getMessage());
        return ResponseEntity.status(e.getHttpStatus())
                .body(new ErrorResponse(e.getCode(), e.getMessage()));
    }

    /** Bean Validation 실패(400)를 필드별 메시지로 변환한다. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("검증 실패: {}", message);
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("VALIDATION_ERROR", message));
    }

    /** 잘못된 JSON 본문·잘못된 enum 값(400)을 변환한다. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("요청 본문 파싱 실패: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("BAD_REQUEST", "요청 본문 형식이 올바르지 않습니다."));
    }

    /** 쿼리 파라미터 타입 불일치(400)를 변환한다. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("파라미터 타입 불일치: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("BAD_REQUEST", "파라미터 값이 올바르지 않습니다."));
    }

    /**
     * 리소스·핸들러 없음(404)을 변환한다.
     * 브라우저가 자동으로 요청하는 /favicon.ico는 debug로만 남긴다.
     */
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(Exception e) {
        if (e instanceof NoResourceFoundException noResource
                && FAVICON_PATH.equals(noResource.getResourcePath())) {
            log.debug("favicon 요청 리소스 없음");
        } else {
            log.warn("요청한 리소스를 찾을 수 없음: {}", e.getMessage());
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("RESOURCE_NOT_FOUND", "요청한 리소스를 찾을 수 없습니다."));
    }

    /** 지원하지 않는 HTTP 메서드(405)를 변환하고 Allow 헤더를 보존한다. */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("지원하지 않는 메서드: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .headers(e.getHeaders())
                .body(new ErrorResponse("METHOD_NOT_ALLOWED", "지원하지 않는 요청 방식입니다."));
    }

    /** 지원하지 않는 미디어 타입(415)을 변환하고 표준 예외의 응답 헤더를 보존한다. */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException e) {
        log.warn("지원하지 않는 미디어 타입: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .headers(e.getHeaders())
                .body(new ErrorResponse("UNSUPPORTED_MEDIA_TYPE", "지원하지 않는 콘텐츠 형식입니다."));
    }

    /** 그 외 예상하지 못한 오류(500)를 변환한다. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("예상하지 못한 오류", e);
        return ResponseEntity.internalServerError()
                .body(new ErrorResponse("INTERNAL_ERROR", "서버 오류가 발생했습니다."));
    }
}

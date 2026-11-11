package com.abetterday.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/** 비즈니스 규칙 위반·검증 오류를 던질 때 사용하는 전용 예외. */
@Getter
public class BusinessException extends RuntimeException {

    private final String code;
    private final HttpStatus httpStatus;

    /** code·message·httpStatus를 지정해 예외를 생성한다. */
    public BusinessException(String code, String message, HttpStatus httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }
}

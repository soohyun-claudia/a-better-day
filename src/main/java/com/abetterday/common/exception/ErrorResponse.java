package com.abetterday.common.exception;

/** 오류 응답 본문을 나타내는 record. */
public record ErrorResponse(String code, String message) {
}

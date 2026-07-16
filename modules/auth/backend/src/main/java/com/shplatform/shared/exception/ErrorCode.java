package com.shplatform.shared.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    INVALID_INPUT(HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED),
    EMAIL_NOT_VERIFIED(HttpStatus.FORBIDDEN),
    FORBIDDEN(HttpStatus.FORBIDDEN),
    NOT_FOUND(HttpStatus.NOT_FOUND),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED),
    CODE_EXPIRED(HttpStatus.BAD_REQUEST),
    INVALID_CODE(HttpStatus.BAD_REQUEST),
    OAUTH2_FAILED(HttpStatus.UNAUTHORIZED),
    OAUTH2_USER_NOT_FOUND(HttpStatus.NOT_FOUND),
    OAUTH2_PROVIDER_ERROR(HttpStatus.BAD_GATEWAY),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS),
    PROVIDER_ALREADY_LINKED(HttpStatus.CONFLICT),
    PROVIDER_NOT_FOUND(HttpStatus.NOT_FOUND),
    LAST_PROVIDER_CANNOT_UNLINK(HttpStatus.BAD_REQUEST),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus httpStatus;

    ErrorCode(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}

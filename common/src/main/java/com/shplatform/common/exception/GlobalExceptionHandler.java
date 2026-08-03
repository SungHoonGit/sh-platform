package com.shplatform.common.exception;

import com.shplatform.common.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Locale;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handle(BusinessException e) {
        var locale = LocaleContextHolder.getLocale();
        var message = resolveMessage(e.getErrorCode().name(), locale);
        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(ApiResponse.error(e.getErrorCode().name(), message));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handle(MethodArgumentNotValidException e) {
        var locale = LocaleContextHolder.getLocale();
        var field = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> messageSource.getMessage(err, locale))
                .orElse("Invalid input");
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error("INVALID_INPUT", field));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handle(NoResourceFoundException e) {
        return errorResponse(HttpStatus.NOT_FOUND, "NOT_FOUND");
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handle(NoHandlerFoundException e) {
        return errorResponse(HttpStatus.NOT_FOUND, "NOT_FOUND");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handle(HttpRequestMethodNotSupportedException e) {
        return errorResponse(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handle(MethodArgumentTypeMismatchException e) {
        return errorResponse(HttpStatus.BAD_REQUEST, "INVALID_INPUT");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handle(HttpMessageNotReadableException e) {
        return errorResponse(HttpStatus.BAD_REQUEST, "INVALID_INPUT");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handle(HttpMediaTypeNotSupportedException e) {
        log.warn("Unsupported media type", e);
        return errorResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "INVALID_INPUT");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handle(Exception e) {
        log.error("Unhandled exception", e);
        return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR");
    }

    private ResponseEntity<ApiResponse<Void>> errorResponse(HttpStatus status, String code) {
        return ResponseEntity
                .status(status)
                .body(ApiResponse.error(code, resolveMessage(code, LocaleContextHolder.getLocale())));
    }

    private String resolveMessage(String code, Locale locale) {
        try {
            return messageSource.getMessage(code, null, locale);
        } catch (NoSuchMessageException e) {
            return code;
        }
    }
}

package com.shplatform.common.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.shplatform.common.dto.ApiResponse;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private MessageSource messageSource;

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler(messageSource);
        LocaleContextHolder.setLocale(Locale.KOREAN);
    }

    @AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void businessException_returnsMappedStatusAndCode() {
        when(messageSource.getMessage("NOT_FOUND", null, Locale.KOREAN))
                .thenReturn("요청하신 리소스를 찾을 수 없습니다.");

        var response = handler.handle(new BusinessException(ErrorCode.NOT_FOUND));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("NOT_FOUND");
        assertThat(response.getBody().message()).isEqualTo("요청하신 리소스를 찾을 수 없습니다.");
    }

    @Test
    void noResourceFound_returns404Json() {
        when(messageSource.getMessage("NOT_FOUND", null, Locale.KOREAN))
                .thenReturn("NOT_FOUND_MSG");

        var response = handler.handle(new NoResourceFoundException(HttpMethod.GET, "/path"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("NOT_FOUND");
    }

    @Test
    void noHandlerFound_returns404Json() {
        when(messageSource.getMessage("NOT_FOUND", null, Locale.KOREAN))
                .thenReturn("NOT_FOUND_MSG");

        var response = handler.handle(new NoHandlerFoundException("GET", "/path", null));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("NOT_FOUND");
    }

    @Test
    void methodNotSupported_returns405Json() {
        when(messageSource.getMessage("METHOD_NOT_ALLOWED", null, Locale.KOREAN))
                .thenReturn("METHOD_NOT_ALLOWED_MSG");

        var response = handler.handle(
                new HttpRequestMethodNotSupportedException("POST"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("METHOD_NOT_ALLOWED");
    }

    @Test
    void typeMismatch_returns400Json() {
        when(messageSource.getMessage("INVALID_INPUT", null, Locale.KOREAN))
                .thenReturn("INVALID_INPUT_MSG");

        var response = handler.handle(
                new MethodArgumentTypeMismatchException("1", Long.class, "page", null, null));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INVALID_INPUT");
    }

    @Test
    void unreadableBody_returns400Json() {
        when(messageSource.getMessage("INVALID_INPUT", null, Locale.KOREAN))
                .thenReturn("INVALID_INPUT_MSG");

        var response = handler.handle(new HttpMessageNotReadableException("bad json"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INVALID_INPUT");
    }

    @Test
    void genericException_returns500Json() {
        when(messageSource.getMessage("INTERNAL_ERROR", null, Locale.KOREAN))
                .thenReturn("INTERNAL_ERROR_MSG");

        var response = handler.handle(new RuntimeException("boom"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INTERNAL_ERROR");
    }

    @Test
    void missingMessage_fallsBackToCode() {
        when(messageSource.getMessage("NOT_FOUND", null, Locale.KOREAN))
                .thenThrow(new NoSuchMessageException("NOT_FOUND"));

        var response = handler.handle(new NoResourceFoundException(HttpMethod.GET, "/path"));

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("NOT_FOUND");
        assertThat(response.getBody().message()).isEqualTo("NOT_FOUND");
    }
}

package com.shplatform.shared.exception;

import com.shplatform.shared.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * auth 모듈의 {@link BusinessException}을 HTTP 응답으로 변환한다.
 *
 * <p>common 모듈의 GlobalExceptionHandler는 common.exception.BusinessException만
 * 처리하므로, auth 자체 예외 계열을 위한 별도 핸들러가 필요하다.
 * 이 핸들러가 없으면 비즈니스 예외가 500 Internal Server Error로 응답된다.
 */
@RestControllerAdvice
public class AuthExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(AuthExceptionHandler.class);

    private final MessageSource messageSource;

    public AuthExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /**
     * (명령형) BusinessException을 ErrorCode의 HTTP 상태와 현지화 메시지로 변환한다.
     *
     * @param e 발생한 비즈니스 예외
     * @return 상태 코드와 에러 본문이 담긴 응답
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handle(BusinessException e) {
        var locale = LocaleContextHolder.getLocale();
        var code = e.getErrorCode().name();
        String message;
        try {
            message = messageSource.getMessage(code, e.getArgs(), locale);
        } catch (NoSuchMessageException ex) {
            message = code;
        }
        log.warn("[AUTH] business exception: code={}", code);
        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(ApiResponse.error(code, message));
    }
}

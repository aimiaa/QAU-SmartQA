package com.aimi.exception;

import com.aimi.result.Result;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("请求参数不正确");
        return Result.error(message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleConstraintViolation(ConstraintViolationException exception) {
        return Result.error(exception.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException exception) {
        ErrorCode errorCode = exception.getErrorCode() != null
                ? exception.getErrorCode()
                : ErrorCode.INTERNAL_ERROR;
        HttpStatus status = HttpStatus.resolve(errorCode.getCode());
        if (status == null) {
            status = HttpStatus.BAD_REQUEST;
        }
        return ResponseEntity.status(status).body(Result.error(errorCode, exception.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleUnexpectedException(Exception exception) {
        log.error("Unhandled error", exception);
        return Result.error("AI 服务暂时不可用，请稍后重试");
    }
}

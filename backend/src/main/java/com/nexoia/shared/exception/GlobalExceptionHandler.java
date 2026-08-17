package com.nexoia.shared.exception;

import com.nexoia.shared.api.BaseResponse;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({
            ConflictApplicationException.class,
            UnauthorizedApplicationException.class
    })
    public ResponseEntity<BaseResponse<Void>> handleCategorizedApplicationException(
            ApplicationException exception) {
        return applicationErrorResponse(exception);
    }

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<BaseResponse<Void>> handleApplicationException(
            ApplicationException exception) {
        return applicationErrorResponse(exception);
    }

    private ResponseEntity<BaseResponse<Void>> applicationErrorResponse(
            ApplicationException exception) {
        log.warn("Application request failed: {}", exception.getMessage());

        return ResponseEntity
                .status(exception.getStatus())
                .body(BaseResponse.error(exception.getStatus().value(), exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<Void>> handleValidationException(
            MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .distinct()
                .collect(Collectors.joining("; "));

        return ResponseEntity.badRequest().body(BaseResponse.error(400, message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Void>> handleUnexpectedException(Exception exception) {
        InternalApplicationException internalException = new InternalApplicationException(exception);
        log.error("Unexpected application failure", exception);

        return ResponseEntity
                .status(internalException.getStatus())
                .body(BaseResponse.error(
                        internalException.getStatus().value(), internalException.getMessage()));
    }
}

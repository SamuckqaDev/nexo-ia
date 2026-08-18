package com.nexoia.shared.exception;

import com.nexoia.shared.api.BaseResponse;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
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
        log.warn("[NEXO-BACK][ERROR] Application request failed status={} message={}", exception.getStatus(), exception.getMessage());

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

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<BaseResponse<Void>> handleTypeMismatchException(
            MethodArgumentTypeMismatchException exception) {
        // A request parameter that cannot be bound — an unknown enum value, a malformed UUID — is a
        // client error, not an internal one. The parameter name is safe; the raw value and the
        // target type are not echoed back.
        return ResponseEntity.badRequest().body(BaseResponse.error(
                400, "Parameter '" + exception.getName() + "' has an invalid value"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Void>> handleUnexpectedException(Exception exception) {
        InternalApplicationException internalException = new InternalApplicationException(exception);
        log.error("[NEXO-BACK][ERROR] Unexpected application failure", exception);

        return ResponseEntity
                .status(internalException.getStatus())
                .body(BaseResponse.error(
                        internalException.getStatus().value(), internalException.getMessage()));
    }
}

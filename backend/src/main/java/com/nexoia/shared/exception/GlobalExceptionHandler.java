package com.nexoia.shared.exception;

import com.nexoia.shared.api.BaseResponse;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.security.access.AccessDeniedException;
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

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<BaseResponse<Void>> handleAccessDeniedException(AccessDeniedException exception) {
        // A method-security denial reaches the advice rather than the filter-chain handler. It is an
        // authorization failure, not an internal error, so it must answer 403 — never 500.
        log.warn("[NEXO-BACK][ERROR] Access denied");

        return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                .body(BaseResponse.error(403, "You are not allowed to perform this action"));
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

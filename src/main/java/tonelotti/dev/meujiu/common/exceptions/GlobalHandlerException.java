package tonelotti.dev.meujiu.common.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import tonelotti.dev.meujiu.common.exceptions.ErrorValidationResponse.FieldErrorResponse;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalHandlerException {

    private static final Logger log = LoggerFactory.getLogger(GlobalHandlerException.class);

    @ExceptionHandler(EmailAlreadyUsedException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyUsedException(
            HttpServletRequest request, EmailAlreadyUsedException ex) {

        return createErrorResponse(
                HttpStatus.CONFLICT,
                "Data conflict",
                ex.getMessage(),
                request);

    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            HttpServletRequest request, HttpMessageNotReadableException ex) {

        return createErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Data type not supported",
                "The type informed not supported in the application",
                request
        );
    }

    @ExceptionHandler(InvalidPeriodException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPeriodException(
            HttpServletRequest request, InvalidPeriodException ex) {

        return createErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Date period not valid",
                "The date informed not supported, period invalid",
                request
        );
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedException(
            HttpServletRequest request, UnauthorizedException ex) {

        return createErrorResponse(
                HttpStatus.UNAUTHORIZED,
                "Unauthorized",
                ex.getMessage(),
                request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(
            HttpServletRequest request, DataIntegrityViolationException ex) {

        log.warn("Data integrity violation at {}: {}",
                request.getRequestURI(), ex.getMostSpecificCause().getMessage());

        return createErrorResponse(
                HttpStatus.CONFLICT,
                "Data integrity violation",
                "The operation conflicts with existing data or violates a database constraint",
                request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            HttpServletRequest request, ResourceNotFoundException ex
    ) {

        return createErrorResponse(
                HttpStatus.NOT_FOUND,
                "Resource Not Found",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorValidationResponse> handleMethodArgumentNotValidException(
            HttpServletRequest request, MethodArgumentNotValidException ex
    ) {

        List<FieldErrorResponse> errors = ex
                .getBindingResult()
                .getFieldErrors()
                .stream()
                .map((FieldError fieldError) -> new FieldErrorResponse(fieldError.getField(), fieldError.getDefaultMessage()))
                .toList();

        return createErrorValidationResponse(
                HttpStatus.BAD_REQUEST,
                "Error of validation",
                "One or more fields are invalid",
                errors,
                request);
    }

    private ResponseEntity<ErrorResponse> createErrorResponse(
            HttpStatus status, String titleError, String message, HttpServletRequest request) {

        ErrorResponse error = new ErrorResponse(
                Instant.now(),
                status.value(),
                titleError,
                message,
                request.getRequestURI());

        return ResponseEntity.status(status).body(error);
    }

    private ResponseEntity<ErrorValidationResponse> createErrorValidationResponse(
            HttpStatus statusHttp, String titleError, String message,
            List<FieldErrorResponse> errors, HttpServletRequest request) {

        ErrorValidationResponse error = new ErrorValidationResponse(
                Instant.now(),
                statusHttp.value(),
                titleError,
                message,
                request.getRequestURI(),
                errors);

        return ResponseEntity.status(statusHttp).body(error);
    }
}

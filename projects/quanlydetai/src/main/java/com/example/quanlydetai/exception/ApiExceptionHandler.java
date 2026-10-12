package com.example.quanlydetai.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.*;

/** JSON error contract for REST; never serializes entities, rejected values or exception causes. */
@RestControllerAdvice(basePackages = "com.example.quanlydetai.controller.api")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        var problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.BAD_REQUEST, "Dữ liệu nhập chưa hợp lệ.");
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult()
                .getFieldErrors()
                .forEach(e -> fields.putIfAbsent(e.getField(), e.getDefaultMessage()));
        problem.setProperty("errors", fields);
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ProblemDetail> business(BusinessException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> denied(AccessDeniedException ex) {
        return error(HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này.");
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ProblemDetail> database(DataAccessException ex) {
        return error(HttpStatus.CONFLICT, DatabaseErrorMessages.extractRootCauseMessage(ex));
    }

    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> invalid(Exception ex) {
        return error(HttpStatus.BAD_REQUEST, "Dữ liệu nhập chưa hợp lệ.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> unexpected(Exception ex) {
        String reference = UUID.randomUUID().toString();
        log.error("Unhandled API error {}", reference, ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Đã xảy ra lỗi. Mã tra cứu: " + reference);
    }

    private ResponseEntity<ProblemDetail> error(HttpStatus status, String detail) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(ProblemDetail.forStatusAndDetail(status, detail));
    }
}

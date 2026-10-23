package com.example.quanlydetai.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;

import java.util.UUID;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ModelAttribute("currentPath")
    public String currentPath(jakarta.servlet.http.HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DataAccessException.class)
    public ModelAndView database(DataAccessException ex) {
        return error(HttpStatus.CONFLICT, DatabaseErrorMessages.extractRootCauseMessage(ex));
    }

    @ExceptionHandler(BusinessException.class)
    public ModelAndView business(BusinessException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ModelAndView denied(AccessDeniedException ex) {
        return error(HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này.");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Object tooLarge(
            MaxUploadSizeExceededException ex, jakarta.servlet.http.HttpServletRequest request) {
        if (isApi(request))
            return apiError(HttpStatus.CONTENT_TOO_LARGE, "File phải nhỏ hơn 20 MiB.");
        return error(HttpStatus.CONTENT_TOO_LARGE, "File phải nhỏ hơn 20 MiB.");
    }

    @ExceptionHandler({
        org.springframework.web.servlet.resource.NoResourceFoundException.class,
        org.springframework.web.servlet.NoHandlerFoundException.class
    })
    public Object notFound(Exception ex, jakarta.servlet.http.HttpServletRequest request) {
        if (isApi(request)) return apiError(HttpStatus.NOT_FOUND, "Không tìm thấy API yêu cầu.");
        return error(HttpStatus.NOT_FOUND, "Không tìm thấy trang yêu cầu.");
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public Object methodNotAllowed(Exception ex, jakarta.servlet.http.HttpServletRequest request) {
        if (isApi(request))
            return apiError(
                    HttpStatus.METHOD_NOT_ALLOWED, "Phương thức yêu cầu không được hỗ trợ.");
        return error(HttpStatus.METHOD_NOT_ALLOWED, "Phương thức yêu cầu không được hỗ trợ.");
    }

    @ExceptionHandler({
        jakarta.validation.ConstraintViolationException.class,
        org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
        org.springframework.web.bind.MissingServletRequestParameterException.class,
        org.springframework.web.multipart.support.MissingServletRequestPartException.class,
        org.springframework.web.bind.MethodArgumentNotValidException.class
    })
    public ModelAndView invalidInput(Exception ex) {
        return error(HttpStatus.BAD_REQUEST, "Dữ liệu nhập chưa hợp lệ. Vui lòng kiểm tra lại.");
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView unexpected(Exception ex) {
        String reference = UUID.randomUUID().toString();
        log.error("Unhandled request error {}", reference, ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Đã xảy ra lỗi. Mã tra cứu: " + reference);
    }

    private boolean isApi(jakarta.servlet.http.HttpServletRequest request) {
        return request.getRequestURI()
                .substring(request.getContextPath().length())
                .startsWith("/api/");
    }

    private org.springframework.http.ResponseEntity<org.springframework.http.ProblemDetail>
            apiError(HttpStatus status, String detail) {
        return org.springframework.http.ResponseEntity.status(status)
                .contentType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON)
                .body(org.springframework.http.ProblemDetail.forStatusAndDetail(status, detail));
    }

    private ModelAndView error(HttpStatus status, String message) {
        var view = new ModelAndView("error/operation");
        view.setStatus(status);
        view.addObject("errorMessage", message);
        return view;
    }
}

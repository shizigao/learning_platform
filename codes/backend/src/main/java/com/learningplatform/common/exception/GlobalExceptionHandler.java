/* 文件职责：统一处理Global异常处理器场景并转换为平台约定的结果。
 * 所属模块：统一协议、异常、配置与跨领域基础设施；所在分层：异常处理层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.common.exception;

import com.learningplatform.common.api.ApiResponse;
import com.learningplatform.common.api.ErrorCode;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
/**
 * 统一处理Global异常处理器场景并转换为平台约定的结果。
 *
 * <p>职责边界：遵守 统一协议、异常、配置与跨领域基础设施 模块的职责边界。</p>
 */
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    /** 执行 handleBusinessException 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ApiResponse.failure(errorCode, exception.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    /** 执行 handleAccessDeniedException 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException exception) {
        ErrorCode errorCode = ErrorCode.FORBIDDEN;
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ApiResponse.failure(errorCode));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    /** 执行 handleValidationException 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(
            MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        ErrorCode errorCode = ErrorCode.VALIDATION_FAILED;
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ApiResponse.failure(errorCode, errorCode.getMessage(), errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    /** 执行 handleConstraintViolationException 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(
            ConstraintViolationException exception) {
        ErrorCode errorCode = ErrorCode.VALIDATION_FAILED;
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ApiResponse.failure(errorCode, exception.getMessage()));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class})
    /** 执行 handleBadRequest 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception exception) {
        ErrorCode errorCode = ErrorCode.BAD_REQUEST;
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ApiResponse.failure(errorCode));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    /** 执行 handleMaxUploadSizeExceededException 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException exception) {
        ErrorCode errorCode = ErrorCode.PAYLOAD_TOO_LARGE;
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ApiResponse.failure(
                        errorCode,
                        "上传文件大小超出服务器限制，请选择符合页面大小限制的文件"
                ));
    }

    @ExceptionHandler(Exception.class)
    /** 执行 handleUnexpectedException 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception exception) {
        log.error("Unhandled server error", exception);
        ErrorCode errorCode = ErrorCode.INTERNAL_ERROR;
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ApiResponse.failure(errorCode));
    }
}

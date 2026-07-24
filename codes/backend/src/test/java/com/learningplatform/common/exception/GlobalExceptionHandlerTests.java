package com.learningplatform.common.exception;

import com.learningplatform.common.api.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTests {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void returnsClearPayloadTooLargeResponseForMultipartLimit() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleMaxUploadSizeExceededException(
                        new MaxUploadSizeExceededException(200L * 1024 * 1024)
                );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(41300);
        assertThat(response.getBody().message()).contains("文件大小超出");
    }
}

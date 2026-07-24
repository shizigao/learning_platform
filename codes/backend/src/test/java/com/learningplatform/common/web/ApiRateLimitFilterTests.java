package com.learningplatform.common.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningplatform.common.config.ApiRateLimitProperties;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class ApiRateLimitFilterTests {
    private final ObjectMapper objectMapper =
            new ObjectMapper().findAndRegisterModules();

    @Test
    void limitsAuthenticationRequestsAndReturnsRetryAfter() throws Exception {
        ApiRateLimitFilter filter = filter(10, 1, 10);

        MockHttpServletResponse first = execute(
                filter,
                request("POST", "/api/auth/login", "10.0.0.1")
        );
        MockHttpServletResponse second = execute(
                filter,
                request("POST", "/api/auth/login", "10.0.0.1")
        );

        assertThat(first.getStatus()).isEqualTo(204);
        assertThat(second.getStatus()).isEqualTo(429);
        assertThat(second.getHeader("Retry-After")).isEqualTo("60");
        assertThat(second.getHeader("X-Content-Type-Options"))
                .isEqualTo("nosniff");
        assertThat(second.getHeader("X-Frame-Options"))
                .isEqualTo("DENY");
        assertThat(second.getContentAsString())
                .contains("\"code\":42900")
                .doesNotContain("10.0.0.1");
    }

    @Test
    void keepsAuthUploadAndGeneralBudgetsIndependent() throws Exception {
        ApiRateLimitFilter filter = filter(1, 1, 1);

        assertThat(execute(
                filter,
                request("GET", "/api/contents", "10.0.0.2")
        ).getStatus()).isEqualTo(204);
        assertThat(execute(
                filter,
                request("POST", "/api/auth/login", "10.0.0.2")
        ).getStatus()).isEqualTo(204);

        MockHttpServletRequest upload = request(
                "POST",
                "/api/publisher/contents/1/files",
                "10.0.0.2"
        );
        upload.setContentType("multipart/form-data; boundary=test");
        assertThat(execute(filter, upload).getStatus()).isEqualTo(204);

        assertThat(execute(
                filter,
                request("GET", "/api/contents/1", "10.0.0.2")
        ).getStatus()).isEqualTo(429);
    }

    @Test
    void excludesHealthOptionsAndDisabledConfiguration() throws Exception {
        ApiRateLimitFilter enabled = filter(1, 1, 1);
        assertThat(execute(
                enabled,
                request("GET", "/api/health", "10.0.0.3")
        ).getStatus()).isEqualTo(204);
        assertThat(execute(
                enabled,
                request("OPTIONS", "/api/auth/login", "10.0.0.3")
        ).getStatus()).isEqualTo(204);

        ApiRateLimitFilter disabled = new ApiRateLimitFilter(
                new ApiRateLimitProperties(false, 1, 1, 1),
                objectMapper
        );
        assertThat(execute(
                disabled,
                request("GET", "/api/contents", "10.0.0.4")
        ).getStatus()).isEqualTo(204);
        assertThat(execute(
                disabled,
                request("GET", "/api/contents", "10.0.0.4")
        ).getStatus()).isEqualTo(204);
    }

    private ApiRateLimitFilter filter(
            int general,
            int auth,
            int upload
    ) {
        return new ApiRateLimitFilter(
                new ApiRateLimitProperties(
                        true,
                        general,
                        auth,
                        upload
                ),
                objectMapper
        );
    }

    private MockHttpServletRequest request(
            String method,
            String path,
            String address
    ) {
        MockHttpServletRequest request =
                new MockHttpServletRequest(method, path);
        request.setRemoteAddr(address);
        return request;
    }

    private MockHttpServletResponse execute(
            ApiRateLimitFilter filter,
            MockHttpServletRequest request
    ) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (ignoredRequest, targetResponse) ->
                ((MockHttpServletResponse) targetResponse).setStatus(204);
        filter.doFilter(request, response, chain);
        return response;
    }
}

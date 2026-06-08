package com.cs.accountsvc.config;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import com.cs.accountsvc.dto.ApiCodes;
import com.cs.accountsvc.dto.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * POC internal-access guard for Account Service account endpoints.
 *
 * <p>The interceptor is registered only for {@code /accounts/**}. It denies
 * direct calls that do not include the expected Event Gateway caller header and
 * shared token header. Health endpoints remain outside this guard so local
 * smoke checks and load balancers can still verify service liveness.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InternalAccessInterceptor implements HandlerInterceptor {

    private final InternalAccessProperties properties;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    /**
     * Validates internal caller headers before controller execution.
     *
     * @param request incoming servlet request
     * @param response outgoing servlet response
     * @param handler selected Spring MVC handler
     * @return {@code true} when the request can continue to the controller
     * @throws Exception when writing the denial response fails
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String caller = request.getHeader(properties.callerHeader());
        String token = request.getHeader(properties.tokenHeader());
        boolean callerAllowed = properties.allowedCaller().equals(caller);
        boolean tokenAllowed = properties.token().equals(token);

        if (callerAllowed && tokenAllowed) {
            log.debug("Accepted internal Account Service request method={} path={} caller={}",
                    request.getMethod(), request.getRequestURI(), caller);
            return true;
        }

        log.warn(
                "Denied Account Service request because internal access headers are invalid method={} path={} "
                        + "callerHeaderPresent={} tokenHeaderPresent={} callerAllowed={} tokenAllowed={}",
                request.getMethod(),
                request.getRequestURI(),
                caller != null && !caller.isBlank(),
                token != null && !token.isBlank(),
                callerAllowed,
                tokenAllowed
        );
        writeForbiddenResponse(response);
        return false;
    }

    /**
     * Writes the standard Account Service error envelope for denied POC internal
     * access requests.
     *
     * @param response servlet response to populate
     * @throws Exception when the JSON response cannot be written
     */
    private void writeForbiddenResponse(HttpServletResponse response) throws Exception {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(clock),
                HttpStatus.FORBIDDEN.value(),
                ApiCodes.INTERNAL_ACCESS_DENIED,
                "Account Service endpoints can only be called by Event Gateway.",
                List.of("Missing or invalid internal caller headers.")
        );
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}

package com.cs.accountsvc.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC infrastructure configuration for Account Service.
 *
 * <p>The current POC uses this configuration to attach the internal-access
 * interceptor to account API endpoints only. Public health endpoints and
 * actuator endpoints are intentionally excluded from this application-level
 * guard.</p>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final InternalAccessInterceptor internalAccessInterceptor;

    /**
     * Registers Account Service MVC interceptors.
     *
     * @param registry Spring MVC interceptor registry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        log.info("Registering internal access interceptor for /accounts/** endpoints");
        registry.addInterceptor(internalAccessInterceptor)
                .addPathPatterns("/accounts/**");
    }
}

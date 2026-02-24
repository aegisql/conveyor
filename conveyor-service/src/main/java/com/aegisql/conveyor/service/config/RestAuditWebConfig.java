package com.aegisql.conveyor.service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class RestAuditWebConfig implements WebMvcConfigurer {

    private final RestAuditInterceptor restAuditInterceptor;

    public RestAuditWebConfig(RestAuditInterceptor restAuditInterceptor) {
        this.restAuditInterceptor = restAuditInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (!restAuditInterceptor.isEnabled()) {
            return;
        }
        registry.addInterceptor(restAuditInterceptor)
                .addPathPatterns(
                        "/api/**",
                        "/part/**",
                        "/static-part/**",
                        "/command/**",
                        "/dashboard/watch",
                        "/dashboard/test/**",
                        "/dashboard/admin/**"
                );
    }
}

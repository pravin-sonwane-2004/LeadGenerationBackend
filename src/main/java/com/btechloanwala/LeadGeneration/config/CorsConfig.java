package com.btechloanwala.LeadGeneration.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS configuration for the React development server.
 *
 * <p>Only the local dev origin {@code http://localhost:5173} is allowed — never a bare
 * {@code allowedOrigins("*")} as a permanent setting. Replace with the real production
 * frontend domain when deployed.</p>
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5173")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
    }
}
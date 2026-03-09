package com.riskmanagement.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    // Set ALLOWED_ORIGINS env var in production (comma-separated list of origin patterns).
    // For Azure Container Apps, include the specific app URL, e.g.:
    //   https://my-frontend.livelyforest-abc123.eastus.azurecontainerapps.io
    // For Azure App Service, include the app URL, e.g.:
    //   https://my-frontend.azurewebsites.net
    @Value("${app.cors.allowed-origins:http://localhost:4200,http://localhost,https://*.azurewebsites.net,https://*.azurecontainerapps.io}")
    private String allowedOriginsEnv;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(allowedOriginsEnv.split(","))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}

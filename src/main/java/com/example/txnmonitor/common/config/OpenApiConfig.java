package com.example.txnmonitor.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI transactionMonitoringOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Transaction Monitoring & Alerts API")
                        .version("v1.0")
                        .description("REST API for recording transactions, evaluating rules, and managing alerts."));
    }
}


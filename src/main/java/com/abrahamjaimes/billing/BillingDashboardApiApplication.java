package com.abrahamjaimes.billing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class BillingDashboardApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(BillingDashboardApiApplication.class, args);
    }
}

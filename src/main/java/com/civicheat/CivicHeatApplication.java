package com.civicheat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Main entrypoint for the Civic Issue Heatmap application.
 */
@SpringBootApplication
@EnableConfigurationProperties
public class CivicHeatApplication {

    public static void main(String[] args) {
        SpringApplication.run(CivicHeatApplication.class, args);
    }
}

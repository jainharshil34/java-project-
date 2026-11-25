package com.civicheat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration enabling scheduled background tasks for severity recalculation and maintenance.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}

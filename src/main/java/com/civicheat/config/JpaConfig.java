package com.civicheat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA configuration enabling entity auditing for created and modified timestamps.
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}

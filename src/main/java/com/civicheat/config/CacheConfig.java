package com.civicheat.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Cache configuration enabling low-latency cached analytics and heatmap tiles.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CACHE_HEATMAP_POINTS = "heatmap_points";
    public static final String CACHE_HEATMAP_HOTSPOTS = "heatmap_hotspots";
    public static final String CACHE_HEATMAP_STATS = "heatmap_stats";

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
                CACHE_HEATMAP_POINTS,
                CACHE_HEATMAP_HOTSPOTS,
                CACHE_HEATMAP_STATS
        );
    }
}

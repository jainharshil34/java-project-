package com.civicheat.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Locale;

/**
 * Detects whether the underlying database supports PostGIS spatial functions (e.g. ST_DWithin).
 * This prevents executing invalid native PostGIS queries on in-memory / H2 databases,
 * which would mark Spring/Hibernate transactions as rollback-only.
 */
@Slf4j
@Component
public class SpatialDatabaseDetector {

    private final boolean postgisSupported;

    @Autowired
    public SpatialDatabaseDetector(DataSource dataSource) {
        boolean supported = false;
        try (Connection conn = dataSource.getConnection()) {
            String dbProduct = conn.getMetaData().getDatabaseProductName();
            log.info("Detected database engine: {}", dbProduct);
            if (dbProduct != null && dbProduct.toLowerCase(Locale.ROOT).contains("postgres")) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("SELECT ST_DWithin(ST_MakePoint(0,0)::geography, ST_MakePoint(0,0)::geography, 1)");
                    supported = true;
                    log.info("PostGIS spatial functions verified and enabled.");
                } catch (Exception e) {
                    log.warn("PostgreSQL detected but PostGIS functions not available: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("Could not inspect database capabilities for PostGIS: {}", e.getMessage());
        }
        this.postgisSupported = supported;
    }

    public SpatialDatabaseDetector(boolean postgisSupported) {
        this.postgisSupported = postgisSupported;
    }

    public boolean isPostgisSupported() {
        return postgisSupported;
    }
}

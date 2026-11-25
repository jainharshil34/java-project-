package com.civicheat.engine.severity;

import com.civicheat.enums.SeverityLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * Result model representing computed severity score, mapped level, and granular factor contributions.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeverityResult {

    private int score;
    private SeverityLevel level;

    @Builder.Default
    private Map<String, Object> breakdown = new HashMap<>();
}

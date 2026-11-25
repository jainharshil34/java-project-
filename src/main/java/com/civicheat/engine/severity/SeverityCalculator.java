package com.civicheat.engine.severity;

import com.civicheat.entity.Complaint;

/**
 * Strategy contract for evaluating civic complaint severity and priority.
 */
public interface SeverityCalculator {

    /**
     * Calculates the composite severity score, risk category level, and factor breakdown.
     *
     * @param complaint The complaint to evaluate
     * @return Calculated SeverityResult
     */
    SeverityResult calculate(Complaint complaint);
}

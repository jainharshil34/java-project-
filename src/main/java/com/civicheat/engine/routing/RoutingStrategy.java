package com.civicheat.engine.routing;

import com.civicheat.entity.Complaint;
import com.civicheat.entity.Department;

/**
 * Strategy contract for determining and assigning the responsible municipal department for an incident.
 */
public interface RoutingStrategy {

    /**
     * Resolves the appropriate Department for a given civic complaint.
     *
     * @param complaint The complaint being routed
     * @return Assigned Department entity
     */
    Department route(Complaint complaint);
}

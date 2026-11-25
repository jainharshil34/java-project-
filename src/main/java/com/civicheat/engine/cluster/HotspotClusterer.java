package com.civicheat.engine.cluster;

import com.civicheat.entity.Complaint;

import java.util.List;

/**
 * Strategy contract for clustering spatial complaints into aggregated hotspot zones.
 */
public interface HotspotClusterer {

    /**
     * Groups complaints into geographic clusters based on cell dimensions.
     *
     * @param complaints     List of complaints to cluster
     * @param cellSizeMeters Width and height of grid cells in meters
     * @return Ranked list of aggregated Hotspot clusters
     */
    List<Hotspot> cluster(List<Complaint> complaints, double cellSizeMeters);
}

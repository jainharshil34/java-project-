package com.civicheat.engine.severity;

import com.civicheat.entity.Complaint;
import com.civicheat.enums.ComplaintStatus;
import com.civicheat.repository.ComplaintRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Scheduled background job recalculating dynamic severity scores and priority levels for all open complaints.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeverityRecalculationScheduler {

    private static final List<ComplaintStatus> OPEN_STATUSES = List.of(
            ComplaintStatus.SUBMITTED,
            ComplaintStatus.VERIFIED,
            ComplaintStatus.ASSIGNED,
            ComplaintStatus.IN_PROGRESS
    );

    private final ComplaintRepository complaintRepository;
    private final SeverityCalculator severityCalculator;

    /**
     * Executes daily recalculation of severity scores for all active open complaints.
     */
    @Scheduled(cron = "${civicheat.severity.recalculate-cron:0 0 2 * * *}")
    @Transactional
    public void recalculateAllOpenComplaints() {
        log.info("Starting daily scheduled severity recalculation for active open complaints...");

        List<Complaint> openComplaints = complaintRepository.findByStatusIn(OPEN_STATUSES);
        log.info("Found {} open complaints to re-evaluate", openComplaints.size());

        int updatedCount = 0;
        for (Complaint complaint : openComplaints) {
            SeverityResult result = severityCalculator.calculate(complaint);
            if (result.getScore() != complaint.getSeverityScore() ||
                    result.getLevel() != complaint.getSeverityLevel()) {
                complaint.setSeverityScore(result.getScore());
                complaint.setSeverityLevel(result.getLevel());
                updatedCount++;
            }
        }

        if (updatedCount > 0) {
            complaintRepository.saveAll(openComplaints);
        }

        log.info("Completed severity recalculation. Updated {} complaints with modified scores.", updatedCount);
    }
}

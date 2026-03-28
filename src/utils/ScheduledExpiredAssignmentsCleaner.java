package utils;

import commands.RBACSystem;
import entities.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class ScheduledExpiredAssignmentsCleaner {
    private final RBACSystem rbacSystem;
    private final BackgroundExecutor executor;
    private final AtomicLong totalExpiredFound = new AtomicLong(0);
    private final AtomicLong totalExpiredMarked = new AtomicLong(0);
    private final AtomicLong taskRuns = new AtomicLong(0);
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final int intervalSeconds;

    public ScheduledExpiredAssignmentsCleaner(RBACSystem rbacSystem, BackgroundExecutor executor, int intervalSeconds) {
        this.rbacSystem = rbacSystem;
        this.executor = executor;
        this.intervalSeconds = intervalSeconds;
    }

    public void start() {
        executor.scheduleAtFixedRate(
                this::executeCleanup,
                intervalSeconds,
                intervalSeconds,
                TimeUnit.SECONDS
        );

        System.out.printf("[%s] Expired assignments cleaner started (interval: %d seconds)%n",
                LocalDateTime.now().format(formatter), intervalSeconds);
    }

    private void executeCleanup() {
        long runNumber = taskRuns.incrementAndGet();
        long startTime = System.currentTimeMillis();

        try {
            List<RoleAssignment> expiredAssignments = rbacSystem.getAssignmentManager().getExpiredAssignments();
            int foundCount = expiredAssignments.size();
            totalExpiredFound.addAndGet(foundCount);

            int markedCount = 0;

            for (RoleAssignment assignment : expiredAssignments) {
                if (assignment instanceof TemporaryAssignment) {
                    rbacSystem.getAssignmentManager().revokeAssignment(assignment.assignmentId());
                    markedCount++;

                    rbacSystem.getAsyncAuditLog().log(
                            "EXPIRED_ASSIGNMENT_MARKED_INACTIVE",
                            "system",
                            "assignments",
                            String.format("Marked inactive: user=%s, role=%s",
                                    assignment.user().username(),
                                    assignment.role().getName())
                    );
                }
            }

            totalExpiredMarked.addAndGet(markedCount);

            long duration = System.currentTimeMillis() - startTime;

            String report = String.format(
                    "[%s] Cleanup run #%d | Found expired: %d | Marked inactive: %d | Total found: %d | Total marked: %d | Duration: %d ms",
                    LocalDateTime.now().format(formatter),
                    runNumber,
                    foundCount,
                    markedCount,
                    totalExpiredFound.get(),
                    totalExpiredMarked.get(),
                    duration
            );

            System.out.println(report);

            rbacSystem.getAsyncAuditLog().log(
                    "EXPIRED_ASSIGNMENTS_REPORT",
                    "system",
                    "cleanup",
                    String.format("Run #%d | Found: %d | Marked: %d | Total found: %d | Total marked: %d",
                            runNumber, foundCount, markedCount, totalExpiredFound.get(), totalExpiredMarked.get())
            );

        } catch (Exception e) {
            System.err.printf("[%s] Error during cleanup: %s%n",
                    LocalDateTime.now().format(formatter),
                    e.getMessage());

            rbacSystem.getAsyncAuditLog().log(
                    "CLEANUP_ERROR",
                    "system",
                    "cleanup",
                    "Error: " + e.getMessage()
            );
        }
    }

    public long getTotalExpiredFound() {
        return totalExpiredFound.get();
    }

    public long getTotalExpiredMarked() {
        return totalExpiredMarked.get();
    }

    public long getTaskRuns() {
        return taskRuns.get();
    }
}
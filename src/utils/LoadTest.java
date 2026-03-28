package utils;

import commands.RBACSystem;
import entities.*;
import repositories.*;
import utils.FormatUtils;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class LoadTest {
    private final RBACSystem rbacSystem;
    private final ExecutorService testExecutor;
    private final AtomicInteger totalOperations = new AtomicInteger(0);
    private final AtomicInteger failedOperations = new AtomicInteger(0);

    public LoadTest() {
        this.rbacSystem = new RBACSystem();
        this.rbacSystem.initialize();
        this.rbacSystem.setCurrentUser("load-tester");
        this.testExecutor = Executors.newFixedThreadPool(20);
    }

    public void runLoadTest() throws InterruptedException {
        System.out.println(FormatUtils.formatHeader("LOAD TEST STARTED"));

        testConcurrentUserCreation(100, 10);

        testConcurrentRoleCreation(50, 10);

        testConcurrentRoleAssignment(150, 15);

        testConcurrentFiltering(200, 20);

        testConcurrentUserUpdate(80, 10);

        testParallelReportGeneration(10);

        testAsyncLoggingStress(500, 10);

        printResults();

        testExecutor.shutdown();
        testExecutor.awaitTermination(10, TimeUnit.SECONDS);
        rbacSystem.shutdown();
    }

    private void testConcurrentUserCreation(int totalUsers, int threads) throws InterruptedException {
        System.out.println("\nTest 1: Concurrent User Creation");
        CountDownLatch latch = new CountDownLatch(threads);
        int usersPerThread = totalUsers / threads;
        AtomicInteger created = new AtomicInteger(0);
        long start = System.currentTimeMillis();

        for (int t = 0; t < threads; t++) {
            final int threadId = t;
            testExecutor.submit(() -> {
                try {
                    for (int i = 0; i < usersPerThread; i++) {
                        String username = "user_t" + threadId + "_" + i + "_" + System.nanoTime();
                        User user = User.create(username, "Test User", username + "@test.com");
                        rbacSystem.getUserManager().add(user);
                        created.incrementAndGet();
                        totalOperations.incrementAndGet();
                    }
                } catch (Exception e) {
                    failedOperations.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        long duration = System.currentTimeMillis() - start;
        System.out.printf("  Created %d users in %d ms (%.2f ops/sec)%n",
                created.get(), duration, created.get() * 1000.0 / duration);
    }

    private void testConcurrentRoleCreation(int totalRoles, int threads) throws InterruptedException {
        System.out.println("\nTest 2: Concurrent Role Creation");
        CountDownLatch latch = new CountDownLatch(threads);
        int rolesPerThread = totalRoles / threads;
        AtomicInteger created = new AtomicInteger(0);
        long start = System.currentTimeMillis();

        for (int t = 0; t < threads; t++) {
            final int threadId = t;
            testExecutor.submit(() -> {
                try {
                    for (int i = 0; i < rolesPerThread; i++) {
                        String roleName = "role_t" + threadId + "_" + i + "_" + System.nanoTime();
                        Role role = new Role(roleName, "Test role");
                        rbacSystem.getRoleManager().add(role);
                        created.incrementAndGet();
                        totalOperations.incrementAndGet();
                    }
                } catch (Exception e) {
                    failedOperations.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        long duration = System.currentTimeMillis() - start;
        System.out.printf("  Created %d roles in %d ms (%.2f ops/sec)%n",
                created.get(), duration, created.get() * 1000.0 / duration);
    }

    private void testConcurrentRoleAssignment(int totalAssignments, int threads) throws InterruptedException {
        System.out.println("\nTest 3: Concurrent Role Assignment");

        List<User> users = new ArrayList<>();
        List<Role> roles = new ArrayList<>();

        for (int i = 0; i < 20; i++) {
            User user = User.create("assign_user_" + i, "Assign User", "assign" + i + "@test.com");
            rbacSystem.getUserManager().add(user);
            users.add(user);
        }

        for (int i = 0; i < 10; i++) {
            Role role = new Role("assign_role_" + i, "Test role");
            rbacSystem.getRoleManager().add(role);
            roles.add(role);
        }

        CountDownLatch latch = new CountDownLatch(threads);
        int assignmentsPerThread = totalAssignments / threads;
        AtomicInteger assigned = new AtomicInteger(0);
        long start = System.currentTimeMillis();

        for (int t = 0; t < threads; t++) {
            testExecutor.submit(() -> {
                try {
                    Random random = new Random();
                    for (int i = 0; i < assignmentsPerThread; i++) {
                        User user = users.get(random.nextInt(users.size()));
                        Role role = roles.get(random.nextInt(roles.size()));

                        AssignmentMetadata metadata = AssignmentMetadata.now("load-tester", "Load test");
                        PermanentAssignment assignment = new PermanentAssignment(user, role, metadata);

                        try {
                            rbacSystem.getAssignmentManager().add(assignment);
                            assigned.incrementAndGet();
                            totalOperations.incrementAndGet();
                        } catch (IllegalArgumentException e) {
                            totalOperations.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    failedOperations.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        long duration = System.currentTimeMillis() - start;
        System.out.printf("  Created %d assignments in %d ms (%.2f ops/sec)%n",
                assigned.get(), duration, assigned.get() * 1000.0 / duration);
    }

    private void testConcurrentFiltering(int totalOps, int threads) throws InterruptedException {
        System.out.println("\nTest 4: Concurrent Filtering (using parallel streams)");
        CountDownLatch latch = new CountDownLatch(threads);
        int opsPerThread = totalOps / threads;
        AtomicInteger filtered = new AtomicInteger(0);
        long start = System.currentTimeMillis();

        for (int t = 0; t < threads; t++) {
            testExecutor.submit(() -> {
                try {
                    for (int i = 0; i < opsPerThread; i++) {
                        int opType = i % 5;

                        switch (opType) {
                            case 0:
                                rbacSystem.getUserManager().findAllParallel().size();
                                break;
                            case 1:
                                rbacSystem.getRoleManager().findAllParallel().size();
                                break;
                            case 2:
                                rbacSystem.getAssignmentManager().getActiveAssignmentsParallel().size();
                                break;
                            case 3:
                                rbacSystem.getAssignmentManager().findByFilterParallel(null);
                                break;
                            case 4:
                                rbacSystem.getUserManager().findAllParallel().stream()
                                        .filter(u -> u.username().contains("user"))
                                        .count();
                                break;
                        }

                        filtered.incrementAndGet();
                        totalOperations.incrementAndGet();
                    }
                } catch (Exception e) {
                    failedOperations.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        long duration = System.currentTimeMillis() - start;
        System.out.printf("  Performed %d filter operations in %d ms (%.2f ops/sec)%n",
                filtered.get(), duration, filtered.get() * 1000.0 / duration);
    }

    private void testConcurrentUserUpdate(int totalUpdates, int threads) throws InterruptedException {
        System.out.println("\nTest 5: Concurrent User Update");

        List<String> usernames = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            String username = "update_user_" + i;
            User user = User.create(username, "Original Name", username + "@test.com");
            rbacSystem.getUserManager().add(user);
            usernames.add(username);
        }

        CountDownLatch latch = new CountDownLatch(threads);
        int updatesPerThread = totalUpdates / threads;
        AtomicInteger updated = new AtomicInteger(0);
        long start = System.currentTimeMillis();

        for (int t = 0; t < threads; t++) {
            testExecutor.submit(() -> {
                try {
                    Random random = new Random();
                    for (int i = 0; i < updatesPerThread; i++) {
                        String username = usernames.get(random.nextInt(usernames.size()));
                        String newFullName = "Updated_" + System.nanoTime();
                        String newEmail = username + "_updated@test.com";

                        try {
                            rbacSystem.getUserManager().update(username, newFullName, newEmail);
                            updated.incrementAndGet();
                            totalOperations.incrementAndGet();
                        } catch (Exception e) {
                            failedOperations.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    failedOperations.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        long duration = System.currentTimeMillis() - start;
        System.out.printf("  Performed %d updates in %d ms (%.2f ops/sec)%n",
                updated.get(), duration, updated.get() * 1000.0 / duration);
    }

    private void testParallelReportGeneration(int totalReports) throws InterruptedException {
        System.out.println("\nTest 6: Parallel Report Generation");
        List<CompletableFuture<String>> futures = new ArrayList<>();
        long start = System.currentTimeMillis();

        for (int i = 0; i < totalReports; i++) {
            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                return new ReportGenerator().generateUserReportParallel(
                        rbacSystem.getUserManager(),
                        rbacSystem.getAssignmentManager()
                );
            }, rbacSystem.getBackgroundExecutor());
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .join();

        long duration = System.currentTimeMillis() - start;
        System.out.printf("  Generated %d reports in %d ms (%.2f reports/sec)%n",
                totalReports, duration, totalReports * 1000.0 / duration);
        totalOperations.addAndGet(totalReports);
    }

    private void testAsyncLoggingStress(int totalLogs, int threads) throws InterruptedException {
        System.out.println("\nTest 7: Async Logging Stress Test");
        CountDownLatch latch = new CountDownLatch(threads);
        int logsPerThread = totalLogs / threads;
        AtomicInteger logged = new AtomicInteger(0);
        long start = System.currentTimeMillis();

        for (int t = 0; t < threads; t++) {
            final int threadId = t;
            testExecutor.submit(() -> {
                try {
                    for (int i = 0; i < logsPerThread; i++) {
                        rbacSystem.getAsyncAuditLog().log(
                                "LOAD_TEST",
                                "tester_" + threadId,
                                "test",
                                "Log entry #" + i
                        );
                        logged.incrementAndGet();
                        totalOperations.incrementAndGet();
                    }
                } catch (Exception e) {
                    failedOperations.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        Thread.sleep(2000);

        long duration = System.currentTimeMillis() - start;
        System.out.printf("  Queued %d log entries in %d ms (%.2f logs/sec)%n",
                logged.get(), duration, logged.get() * 1000.0 / duration);
        System.out.printf("  Audit log persisted entries: %d%n",
                rbacSystem.getAsyncAuditLog().getAll().size());
    }

    private void printResults() {
        System.out.println(FormatUtils.formatHeader("LOAD TEST RESULTS"));
        System.out.printf("Total operations: %d%n", totalOperations.get());
        System.out.printf("Failed operations: %d%n", failedOperations.get());
        System.out.printf("Success rate: %.2f%%%n",
                (totalOperations.get() - failedOperations.get()) * 100.0 / totalOperations.get());

        System.out.println(FormatUtils.formatHeader("System Statistics"));
        System.out.println(rbacSystem.generateStatistics());
    }

    public static void main(String[] args) throws InterruptedException {
        LoadTest loadTest = new LoadTest();
        loadTest.runLoadTest();
    }
}
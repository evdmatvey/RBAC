import entities.*;
import org.junit.jupiter.api.*;
import utils.BackgroundExecutor;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class AsyncAuditLogTest {

    private BackgroundExecutor executor;
    private AsyncAuditLog asyncAuditLog;

    @BeforeEach
    void setUp() {
        executor = new BackgroundExecutor(2);
        asyncAuditLog = new AsyncAuditLog(executor);
    }

    @AfterEach
    void tearDown() {
        asyncAuditLog.shutdown();
        executor.shutdown();
    }

    @Test
    void testLog() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        asyncAuditLog.log("TEST_ACTION", "tester", "target", "details")
                .thenRun(latch::countDown);

        assertTrue(latch.await(5, TimeUnit.SECONDS));

        Thread.sleep(500);

        assertEquals(1, asyncAuditLog.getAll().size());
    }

    @Test
    void testMultipleLogs() throws Exception {
        int logCount = 50;
        CountDownLatch latch = new CountDownLatch(logCount);

        for (int i = 0; i < logCount; i++) {
            asyncAuditLog.log("ACTION_" + i, "tester", "target", "details")
                    .thenRun(latch::countDown);
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));

        Thread.sleep(1000);

        assertEquals(logCount, asyncAuditLog.getAll().size());
    }

    @Test
    void testConcurrentLogs() throws Exception {
        int threadCount = 10;
        int logsPerThread = 20;
        ExecutorService testExecutor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            testExecutor.submit(() -> {
                try {
                    for (int i = 0; i < logsPerThread; i++) {
                        asyncAuditLog.log("CONCURRENT_TEST", "tester_" + threadId, "target", "log_" + i);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(15, TimeUnit.SECONDS));

        Thread.sleep(2000);

        assertEquals(threadCount * logsPerThread, asyncAuditLog.getAll().size());

        testExecutor.shutdown();
        testExecutor.awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    void testGetByPerformer() throws Exception {
        asyncAuditLog.log("ACTION1", "performer1", "target1", "details1");
        asyncAuditLog.log("ACTION2", "performer2", "target2", "details2");
        asyncAuditLog.log("ACTION3", "performer1", "target3", "details3");

        Thread.sleep(1000);

        assertEquals(3, asyncAuditLog.getAll().size());
        assertEquals(2, asyncAuditLog.getByPerformer("performer1").size());
        assertEquals(1, asyncAuditLog.getByPerformer("performer2").size());
    }

    @Test
    void testGetByAction() throws Exception {
        asyncAuditLog.log("CREATE_USER", "admin", "user1", "created");
        asyncAuditLog.log("DELETE_USER", "admin", "user2", "deleted");
        asyncAuditLog.log("CREATE_USER", "manager", "user3", "created");

        Thread.sleep(1000);

        assertEquals(2, asyncAuditLog.getByAction("CREATE_USER").size());
        assertEquals(1, asyncAuditLog.getByAction("DELETE_USER").size());
    }

    @Test
    void testNullArguments() {
        assertThrows(IllegalArgumentException.class, () -> {
            asyncAuditLog.log(null, "performer", "target", "details");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            asyncAuditLog.log("action", null, "target", "details");
        });
    }
}
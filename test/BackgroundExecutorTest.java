import utils.BackgroundExecutor;
import org.junit.jupiter.api.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class BackgroundExecutorTest {

    private BackgroundExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new BackgroundExecutor(2, 0);
    }

    @AfterEach
    void tearDown() {
        executor.shutdown();
    }

    @Test
    void testExecute() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);

        executor.execute(() -> {
            counter.incrementAndGet();
            latch.countDown();
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(1, counter.get());
    }

    @Test
    void testSubmitRunnable() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);

        CompletableFuture<Void> future = executor.submit(() -> {
            counter.incrementAndGet();
            latch.countDown();
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertNull(future.get(1, TimeUnit.SECONDS));
        assertEquals(1, counter.get());
    }

    @Test
    void testSubmitCallable() throws Exception {
        CompletableFuture<String> future = executor.submit(() -> "test result");

        String result = future.get(5, TimeUnit.SECONDS);
        assertEquals("test result", result);
    }

    @Test
    void testSubmitCallableWithException() {
        CompletableFuture<String> future = executor.submit(() -> {
            throw new RuntimeException("Test exception");
        });

        assertThrows(ExecutionException.class, () -> future.get(5, TimeUnit.SECONDS));
    }

    @Test
    void testMultipleTasks() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(10);

        for (int i = 0; i < 10; i++) {
            executor.submit(() -> {
                counter.incrementAndGet();
                latch.countDown();
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        assertEquals(10, counter.get());
    }

    @Test
    void testConcurrentTasks() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        int taskCount = 100;
        CountDownLatch latch = new CountDownLatch(taskCount);

        for (int i = 0; i < taskCount; i++) {
            executor.submit(() -> {
                counter.incrementAndGet();
                latch.countDown();
            });
        }

        assertTrue(latch.await(15, TimeUnit.SECONDS));
        assertEquals(taskCount, counter.get());
    }

    @Test
    void testShutdown() {
        executor.shutdown();
        assertTrue(executor.isShutdown());

        assertThrows(IllegalStateException.class, () -> {
            executor.execute(() -> {});
        });
    }

    @Test
    void testGetExecutorService() {
        ExecutorService service = executor.getExecutorService();
        assertNotNull(service);
        assertFalse(service.isShutdown());
    }
}
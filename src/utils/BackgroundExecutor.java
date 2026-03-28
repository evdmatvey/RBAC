package utils;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class BackgroundExecutor implements Executor {
    private final ExecutorService executorService;
    private final ScheduledExecutorService scheduledExecutorService;
    private final AtomicLong taskCounter = new AtomicLong(0);
    private volatile boolean isShutdown = false;

    public BackgroundExecutor() {
        this(4, 2);
    }

    public BackgroundExecutor(int corePoolSize, int scheduledPoolSize) {
        this.executorService = Executors.newFixedThreadPool(corePoolSize, r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("RBAC-Worker-" + taskCounter.incrementAndGet());
            return t;
        });

        this.scheduledExecutorService = Executors.newScheduledThreadPool(scheduledPoolSize, r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("RBAC-Scheduler-" + taskCounter.incrementAndGet());
            return t;
        });
    }

    @Override
    public void execute(Runnable command) {
        if (isShutdown) {
            throw new IllegalStateException("BackgroundExecutor is shutdown");
        }
        executorService.execute(command);
    }

    public CompletableFuture<Void> submit(Runnable task) {
        if (isShutdown) {
            throw new IllegalStateException("BackgroundExecutor is shutdown");
        }
        return CompletableFuture.runAsync(task, executorService);
    }

    public <T> CompletableFuture<T> submit(Callable<T> task) {
        if (isShutdown) {
            throw new IllegalStateException("BackgroundExecutor is shutdown");
        }
        return CompletableFuture.supplyAsync(() -> {
            try {
                return task.call();
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, executorService);
    }

    public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit unit) {
        if (isShutdown) {
            throw new IllegalStateException("BackgroundExecutor is shutdown");
        }
        return scheduledExecutorService.scheduleAtFixedRate(task, initialDelay, period, unit);
    }

    public void shutdown() {
        isShutdown = true;
        executorService.shutdown();
        scheduledExecutorService.shutdown();

        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
            if (!scheduledExecutorService.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduledExecutorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            scheduledExecutorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public boolean isShutdown() {
        return isShutdown;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }
}
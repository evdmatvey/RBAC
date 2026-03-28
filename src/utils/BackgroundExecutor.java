package utils;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class BackgroundExecutor implements Executor {
    private final ExecutorService executorService;
    private final AtomicLong taskCounter = new AtomicLong(0);
    private volatile boolean isShutdown = false;

    public BackgroundExecutor() {
        this(4);
    }

    public BackgroundExecutor(int corePoolSize) {
        this.executorService = Executors.newFixedThreadPool(corePoolSize, r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("RBAC-Background-" + taskCounter.incrementAndGet());
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

    public ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit) {
        if (isShutdown) {
            throw new IllegalStateException("BackgroundExecutor is shutdown");
        }
        if (executorService instanceof ScheduledExecutorService) {
            return ((ScheduledExecutorService) executorService).schedule(task, delay, unit);
        }
        throw new UnsupportedOperationException("Executor does not support scheduling");
    }

    public void shutdown() {
        isShutdown = true;
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
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